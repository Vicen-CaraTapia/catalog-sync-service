package ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.exception.InvalidCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Requires a dedicated PostgreSQL database. The runner is disabled by the test profile,
 * so each test prepares its database state before invoking the real initialization use case.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CatalogInitializationIT {

    private static final long SNAPSHOT_VERSION = 7L;
    private static final AtomicReference<HttpResponse> RESPONSE = new AtomicReference<>();
    private static final AtomicReference<String> AUTHORIZATION = new AtomicReference<>();
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger();
    private static final HttpServer SERVER = startServer();

    @Autowired
    private InitializeCatalogUseCase initializeCatalog;

    @Autowired
    private ProfessionalCategoryJpaRepository categoryRepository;

    @Autowired
    private ProfessionalJpaRepository professionalRepository;

    @Autowired
    private WeeklyScheduleJpaRepository scheduleRepository;

    @Autowired
    private CatalogSyncStateRepository syncStateRepository;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @DynamicPropertySource
    static void localCatedraRestEndpoint(DynamicPropertyRegistry registry) {
        registry.add("catedra.rest.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @BeforeEach
    void prepareTest() {
        clearCatalog();
        REQUEST_COUNT.set(0);
        AUTHORIZATION.set(null);
        RESPONSE.set(new HttpResponse(503, "application/problem+json", "{}"));
    }

    @AfterEach
    void cleanAfterTest() {
        clearCatalog();
    }

    @Test
    void initializesEmptyCatalogFromAuthenticatedSnapshotAndPersistsVersion() {
        RESPONSE.set(new HttpResponse(200, "application/json", completeSnapshotJson()));

        initializeCatalog.initializeIfNeeded();

        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(AUTHORIZATION.get()).isEqualTo("Bearer test-only-technical-jwt");
        assertThat(appliedVersion()).isEqualTo(SNAPSHOT_VERSION);
        assertThat(categoryRepository.findAll()).singleElement().satisfies(category -> {
            assertThat(category.getId()).isEqualTo(1L);
            assertThat(category.getDescription()).isNull();
            assertThat(category.isEnabled()).isFalse();
            assertThat(category.getUpdatedAt()).isEqualTo(Instant.parse("2026-09-10T13:40:00Z"));
        });
        assertThat(professionalRepository.findAll()).singleElement().satisfies(professional -> {
            assertThat(professional.getId()).isEqualTo(15L);
            assertThat(professional.getCategoryId()).isEqualTo(1L);
            assertThat(professional.isEnabled()).isFalse();
        });
        assertThat(scheduleRepository.findAll()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getId()).isEqualTo(21L);
            assertThat(schedule.getProfessionalId()).isEqualTo(15L);
            assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 0));
            assertThat(schedule.isEnabled()).isFalse();
        });
    }

    @Test
    void doesNotFetchSnapshotWhenEmptyCatalogAlreadyHasAppliedVersion() {
        jdbcTemplate.update("UPDATE catalog_sync_state SET applied_version = ? WHERE state_id = ?",
                12L, CatalogSyncStateEntity.SINGLETON_ID);

        initializeCatalog.initializeIfNeeded();

        assertThat(REQUEST_COUNT).hasValue(0);
        assertThat(appliedVersion()).isEqualTo(12L);
        assertCatalogTablesEmpty();
    }

    @Test
    void httpFailureLeavesCatalogUninitialized() {
        RESPONSE.set(new HttpResponse(503, "application/problem+json",
                "{\"code\":\"CATALOG_UNAVAILABLE\",\"detail\":\"Unavailable\"}"));

        assertThatThrownBy(initializeCatalog::initializeIfNeeded)
                .isInstanceOf(CatedraHttpException.class);

        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(appliedVersion()).isNull();
        assertCatalogTablesEmpty();
    }

    @Test
    void invalidSnapshotMappingLeavesCatalogUninitialized() {
        RESPONSE.set(new HttpResponse(200, "application/json", invalidReferenceSnapshotJson()));

        assertThatThrownBy(initializeCatalog::initializeIfNeeded)
                .isInstanceOf(InvalidCatalogSnapshotException.class);

        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(appliedVersion()).isNull();
        assertCatalogTablesEmpty();
    }

    @Test
    void databaseFailureRollsBackCatalogAndVersion() {
        RESPONSE.set(new HttpResponse(200, "application/json", completeSnapshotJson()));
        jdbcTemplate.execute("ALTER TABLE catalog_sync_state ADD CONSTRAINT "
                + "chk_test_reject_initial_catalog_version CHECK (applied_version IS DISTINCT FROM 7)");

        assertThatThrownBy(initializeCatalog::initializeIfNeeded)
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(appliedVersion()).isNull();
        assertCatalogTablesEmpty();
    }

    private void assertCatalogTablesEmpty() {
        assertThat(categoryRepository.count()).isZero();
        assertThat(professionalRepository.count()).isZero();
        assertThat(scheduleRepository.count()).isZero();
    }

    private Long appliedVersion() {
        return syncStateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow()
                .getAppliedVersion();
    }

    private void clearCatalog() {
        jdbcTemplate.execute("ALTER TABLE catalog_sync_state "
                + "DROP CONSTRAINT IF EXISTS chk_test_reject_initial_catalog_version");
        jdbcTemplate.update("DELETE FROM weekly_schedules");
        jdbcTemplate.update("DELETE FROM professionals");
        jdbcTemplate.update("DELETE FROM professional_categories");
        assertThat(jdbcTemplate.update("UPDATE catalog_sync_state SET applied_version = NULL WHERE state_id = ?",
                CatalogSyncStateEntity.SINGLETON_ID)).isEqualTo(1);
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/synchronization/snapshot", exchange -> {
                REQUEST_COUNT.incrementAndGet();
                AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, RESPONSE.get());
            });
            server.start();
            return server;
        }
        catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void respond(HttpExchange exchange, HttpResponse response) throws IOException {
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", response.contentType());
        exchange.sendResponseHeaders(response.status(), body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static String completeSnapshotJson() {
        return """
                {
                  "snapshotVersion": 7,
                  "generatedAt": "2026-09-10T13:44:59Z",
                  "professionalCategories": [{
                    "id": 1,
                    "name": "Clinical medicine",
                    "description": null,
                    "enabled": false,
                    "createdAt": "2026-07-01T12:00:00Z",
                    "updatedAt": "2026-09-10T13:40:00Z"
                  }],
                  "professionals": [{
                    "id": 15,
                    "categoryId": 1,
                    "firstName": "Ana",
                    "lastName": "Gomez",
                    "enabled": false,
                    "createdAt": "2026-07-01T12:00:00Z",
                    "updatedAt": "2026-09-10T13:40:00Z"
                  }],
                  "weeklySchedules": [{
                    "id": 21,
                    "professionalId": 15,
                    "dayOfWeek": "MONDAY",
                    "startTime": "09:00:00",
                    "endTime": "12:00:00",
                    "slotDurationMinutes": 30,
                    "enabled": false,
                    "createdAt": "2026-07-01T12:00:00Z",
                    "updatedAt": "2026-09-10T13:40:00Z"
                  }]
                }
                """;
    }

    private static String invalidReferenceSnapshotJson() {
        return """
                {
                  "snapshotVersion": 7,
                  "generatedAt": "2026-09-10T13:44:59Z",
                  "professionalCategories": [],
                  "professionals": [{
                    "id": 15,
                    "categoryId": 1,
                    "firstName": "Ana",
                    "lastName": "Gomez",
                    "enabled": true,
                    "createdAt": "2026-07-01T12:00:00Z",
                    "updatedAt": "2026-09-10T13:40:00Z"
                  }],
                  "weeklySchedules": []
                }
                """;
    }

    private record HttpResponse(int status, String contentType, String body) {
    }
}
