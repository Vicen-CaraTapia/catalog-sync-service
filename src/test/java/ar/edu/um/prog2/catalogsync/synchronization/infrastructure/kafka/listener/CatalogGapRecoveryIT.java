package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.StaleCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.InsufficientCatalogSnapshotVersionException;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real HTTP client, mapper, JPA adapters and commits; controlled Redis and acknowledgment. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CatalogGapRecoveryIT {

    private static final AtomicReference<Response> RESPONSE = new AtomicReference<>();
    private static final AtomicReference<Runnable> BEFORE_RESPONSE = new AtomicReference<>(() -> {});
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static final HttpServer SERVER = startServer();
    private static final String CONSTRAINT = "chk_c62_test_reject_snapshot";

    @Autowired
    private CatalogUpdatedListener listener;
    @Autowired
    private ApplyCatalogSnapshotUseCase applySnapshot;
    @Autowired
    private DataSource dataSource;
    @MockitoBean
    private ReadCatalogIncrementalChangesPort changesPort;
    @MockitoSpyBean
    private ProcessedCatalogEventPort processedEvents;
    @MockitoSpyBean
    private FetchCatalogSnapshotPort fetchSnapshot;

    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
    private final String eventId = "c62-test-" + UUID.randomUUID();
    private JdbcTemplate jdbc;
    private boolean cleanupRequired;

    @DynamicPropertySource
    static void localHttp(DynamicPropertyRegistry registry) {
        registry.add("catedra.rest.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }

    @BeforeEach
    void prepare() {
        jdbc = new JdbcTemplate(dataSource);
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        if (database == null || !database.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException("Gap recovery IT requires a dedicated test database");
        }
        cleanupRequired = true;
        clearCatalog();
        jdbc.update("INSERT INTO professional_categories VALUES (1, 'Original', NULL, TRUE, now(), now())");
        jdbc.update("INSERT INTO professionals VALUES (15, 1, 'Original', 'Person', TRUE, now(), now())");
        jdbc.update("INSERT INTO weekly_schedules VALUES (21, 15, 'MONDAY', '09:00', '12:00', 30, TRUE, now(), now())");
        jdbc.update("UPDATE catalog_sync_state SET applied_version = 3 WHERE state_id = 1");
        REQUESTS.set(0);
        BEFORE_RESPONSE.set(() -> {});
        RESPONSE.set(new Response(200, snapshotJson(8)));
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return invocation.callRealMethod();
        }).when(fetchSnapshot).fetchSnapshot();
    }

    @AfterEach
    void cleanup() {
        if (cleanupRequired) {
            BEFORE_RESPONSE.set(() -> {});
            clearCatalog();
            jdbc.update("DELETE FROM processed_catalog_events WHERE event_id = ?", eventId);
        }
    }

    @AfterAll
    static void stopHttp() {
        SERVER.stop(0);
    }

    @Test
    void continuousHistoryUsesIncrementalPathWithoutDownloadingSnapshot() {
        when(changesPort.readMetadata()).thenReturn(metadata(4, 1));
        when(changesPort.readChanges(4)).thenReturn(Optional.of(emptyChanges(4)));
        assertCommittedBeforeAcknowledgment(4, 4);

        listener.onMessage(record(4), acknowledgment);

        assertThat(REQUESTS).hasValue(0);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void tooOldLocalVersionRecoversNewerSnapshotAndRegistersNotifiedVersionBeforeAck() {
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));
        assertCommittedBeforeAcknowledgment(8, 7);

        listener.onMessage(record(7), acknowledgment);

        assertSnapshotApplied(8);
        assertThat(REQUESTS).hasValue(1);
        verify(changesPort, never()).readChanges(4);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void missingRequiredChangesetRecoversWithoutSkippingIt() {
        when(changesPort.readMetadata()).thenReturn(metadata(5, 1));
        when(changesPort.readChanges(4)).thenReturn(Optional.empty());

        listener.onMessage(record(5), acknowledgment);

        assertSnapshotApplied(8);
        verify(changesPort, never()).readChanges(5);
        assertThat(processedVersion()).isEqualTo(5L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void redisAheadOfEventSelectsSnapshotEvenWhenHistoryIsRetained() {
        when(changesPort.readMetadata()).thenReturn(metadata(8, 1));

        listener.onMessage(record(7), acknowledgment);

        assertSnapshotApplied(8);
        verify(changesPort, never()).readChanges(4);
        assertThat(processedVersion()).isEqualTo(7L);
    }

    @Test
    void insufficientSnapshotDoesNotChangeDataVersionOrEvents() {
        var before = state();
        RESPONSE.set(new Response(200, snapshotJson(6)));
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(InsufficientCatalogSnapshotVersionException.class);

        assertFailedWithoutChanges(before);
    }

    @Test
    void fetchFailureDoesNotChangeDataVersionOrEvents() {
        var before = state();
        RESPONSE.set(new Response(503, "{\"code\":\"CATALOG_UNAVAILABLE\"}"));
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(CatedraHttpException.class);

        assertFailedWithoutChanges(before);
    }

    @Test
    void databaseFailureRollsBackSnapshotAndDoesNotRegisterOrAcknowledge() {
        var before = state();
        jdbc.execute("ALTER TABLE catalog_sync_state ADD CONSTRAINT " + CONSTRAINT
                + " CHECK (applied_version IS DISTINCT FROM 8)");
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertFailedWithoutChanges(before);
    }

    @Test
    void concurrentLocalAdvancementDuringFetchRejectsStaleSnapshotWithoutRegistering() {
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));
        BEFORE_RESPONSE.set(() -> applySnapshot.apply(new CatalogSnapshot(9, Instant.EPOCH,
                List.of(new ProfessionalCategory(999L, "Concurrent", null, true,
                        Instant.EPOCH, Instant.EPOCH)), List.of(), List.of())));

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(StaleCatalogSnapshotException.class);

        assertThat(appliedVersion()).isEqualTo(9L);
        assertThat(jdbc.queryForList("SELECT id FROM professional_categories", Long.class)).containsExactly(999L);
        assertThat(processedVersion()).isNull();
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void duplicateAfterRecoverySkipsRedisAndSnapshotAndCanAcknowledgeAgain() {
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));
        listener.onMessage(record(7), acknowledgment);
        var before = state();
        clearInvocations(changesPort);

        listener.onMessage(record(7), acknowledgment);

        assertThat(state()).isEqualTo(before);
        assertThat(REQUESTS).hasValue(1);
        verifyNoInteractions(changesPort);
        verify(acknowledgment, times(2)).acknowledge();
    }

    @Test
    void conflictingProcessedIdFailsBeforeReadingRedisOrFetchingSnapshot() {
        processedEvents.registerProcessed(eventId, 8);
        var before = state();

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(ConflictingCatalogEventException.class);

        assertThat(state()).isEqualTo(before);
        assertThat(processedVersion()).isEqualTo(8L);
        assertThat(REQUESTS).hasValue(0);
        verifyNoInteractions(changesPort);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void registrationFailureLeavesCommittedSnapshotAndRedeliveryCanCompleteWithoutReapplying() {
        when(changesPort.readMetadata()).thenReturn(metadata(7, 4));
        var failure = new IllegalStateException("Controlled registration failure");
        doThrow(failure).when(processedEvents).registerProcessed(eventId, 7);

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment)).isSameAs(failure);

        assertSnapshotApplied(8);
        assertThat(processedVersion()).isNull();
        verify(acknowledgment, never()).acknowledge();

        doCallRealMethod().when(processedEvents).registerProcessed(eventId, 7);
        listener.onMessage(record(7), acknowledgment);

        assertThat(processedVersion()).isEqualTo(7L);
        assertThat(REQUESTS).hasValue(1);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void redisTechnicalFailurePropagatesWithoutSnapshotOrAcknowledgment() {
        var before = state();
        var failure = new CatedraRedisUnavailableException(null);
        when(changesPort.readMetadata()).thenThrow(failure);

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment)).isSameAs(failure);

        assertThat(REQUESTS).hasValue(0);
        assertFailedWithoutChanges(before);
    }

    @Test
    void earlierIncrementRemainsCommittedWhenMissingLaterVersionAndSnapshotFetchFail() {
        when(changesPort.readMetadata()).thenReturn(metadata(6, 1));
        when(changesPort.readChanges(4)).thenReturn(Optional.of(new CatalogIncrementalChanges(4,
                List.of(new ProfessionalCategory(1L, "Committed increment", null, false,
                        Instant.EPOCH, Instant.EPOCH)), List.of(), List.of())));
        when(changesPort.readChanges(5)).thenReturn(Optional.empty());
        RESPONSE.set(new Response(503, "{}"));

        assertThatThrownBy(() -> listener.onMessage(record(6), acknowledgment))
                .isInstanceOf(CatedraHttpException.class);

        assertThat(appliedVersion()).isEqualTo(4L);
        assertThat(jdbc.queryForObject("SELECT name FROM professional_categories WHERE id = 1", String.class))
                .isEqualTo("Committed increment");
        assertThat(processedVersion()).isNull();
        verify(changesPort, never()).readChanges(6);
        verify(acknowledgment, never()).acknowledge();
    }

    private void assertCommittedBeforeAcknowledgment(long catalogVersion, long eventVersion) {
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            // Separate connection: assertions observe committed data rather than an open JPA transaction.
            assertThat(appliedVersion()).isEqualTo(catalogVersion);
            assertThat(processedVersion()).isEqualTo(eventVersion);
            return null;
        }).when(acknowledgment).acknowledge();
    }

    private void assertSnapshotApplied(long version) {
        assertThat(appliedVersion()).isEqualTo(version);
        assertThat(jdbc.queryForObject("SELECT name FROM professional_categories WHERE id = 1", String.class))
                .isEqualTo("Snapshot category");
        assertThat(jdbc.queryForObject("SELECT enabled FROM professionals WHERE id = 15", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT day_of_week FROM weekly_schedules WHERE id = 21", String.class))
                .isEqualTo("FRIDAY");
    }

    private void assertFailedWithoutChanges(List<Object> before) {
        assertThat(state()).isEqualTo(before);
        assertThat(processedVersion()).isNull();
        verify(acknowledgment, never()).acknowledge();
    }

    private List<Object> state() {
        return List.of(appliedVersion(), jdbc.queryForList("SELECT * FROM professional_categories ORDER BY id"),
                jdbc.queryForList("SELECT * FROM professionals ORDER BY id"),
                jdbc.queryForList("SELECT * FROM weekly_schedules ORDER BY id"));
    }

    private Long appliedVersion() {
        return jdbc.queryForObject("SELECT applied_version FROM catalog_sync_state WHERE state_id = 1", Long.class);
    }

    private Long processedVersion() {
        return jdbc.query("SELECT new_version FROM processed_catalog_events WHERE event_id = ?",
                (result, row) -> result.getLong(1), eventId).stream().findFirst().orElse(null);
    }

    private void clearCatalog() {
        jdbc.execute("ALTER TABLE catalog_sync_state DROP CONSTRAINT IF EXISTS " + CONSTRAINT);
        jdbc.update("DELETE FROM weekly_schedules");
        jdbc.update("DELETE FROM professionals");
        jdbc.update("DELETE FROM professional_categories");
        jdbc.update("UPDATE catalog_sync_state SET applied_version = NULL WHERE state_id = 1");
    }

    private ConsumerRecord<String, String> record(long version) {
        return new ConsumerRecord<>("test-catalog-topic", 0, 0L, Long.toString(version), """
                {"eventId":"%s","eventType":"CatalogUpdated","newVersion":%d,
                 "occurredAt":"2026-09-10T13:44:59Z","source":"TEST","schemaVersion":1}
                """.formatted(eventId, version));
    }

    private static CatalogSyncMetadata metadata(long current, long oldest) {
        return new CatalogSyncMetadata(current, oldest, Instant.EPOCH);
    }

    private static CatalogIncrementalChanges emptyChanges(long version) {
        return new CatalogIncrementalChanges(version, List.of(), List.of(), List.of());
    }

    private static String snapshotJson(long version) {
        return """
                {"snapshotVersion":%d,"generatedAt":"2026-09-10T13:44:59Z",
                 "professionalCategories":[{"id":1,"name":"Snapshot category","description":null,
                   "enabled":false,"createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}],
                 "professionals":[{"id":15,"categoryId":1,"firstName":"Snapshot","lastName":"Person",
                   "enabled":false,"createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}],
                 "weeklySchedules":[{"id":21,"professionalId":15,"dayOfWeek":"FRIDAY",
                   "startTime":"10:00:00","endTime":"13:00:00","slotDurationMinutes":20,
                   "enabled":false,"createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}]}
                """.formatted(version);
    }

    private static HttpServer startServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/synchronization/snapshot", exchange -> {
                REQUESTS.incrementAndGet();
                BEFORE_RESPONSE.get().run();
                var response = RESPONSE.get();
                byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type",
                        response.status() == 200 ? "application/json" : "application/problem+json");
                exchange.sendResponseHeaders(response.status(), bytes.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private record Response(int status, String body) {
    }
}
