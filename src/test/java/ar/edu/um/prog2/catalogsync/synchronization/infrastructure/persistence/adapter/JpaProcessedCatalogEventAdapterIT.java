package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.entity.ProcessedCatalogEventEntity;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.repository.ProcessedCatalogEventJpaRepository;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real transactions, isolated contexts, no Catalog runners or external catedra adapters. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaProcessedCatalogEventAdapterIT {

    private ConfigurableApplicationContext context;
    private ProcessedCatalogEventPort port;
    private JdbcTemplate jdbc;
    private final String prefix = "c61-test-" + UUID.randomUUID() + "-";

    @BeforeAll
    void start() {
        String url = System.getenv("CATALOG_DB_URL");
        if (url == null || !url.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException("Processed event IT requires a dedicated test database");
        }
        context = openContext();
        port = context.getBean(ProcessedCatalogEventPort.class);
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).containsIgnoringCase("test");
    }

    @AfterEach
    void cleanRows() {
        jdbc.update("DELETE FROM processed_catalog_events WHERE event_id LIKE ?", prefix + "%");
    }

    @AfterAll
    void close() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void registersAndCommitsBeforeReturning() {
        String id = prefix + "first";
        assertThat(port.findProcessedVersion(id)).isEmpty();
        port.registerProcessed(id, 7);
        // JdbcTemplate obtains an independent connection, outside the adapter transaction.
        assertThat(storedVersion(id)).isEqualTo(7L);
        assertThat(port.findProcessedVersion(id)).hasValue(7);
        var entity = context.getBean(ProcessedCatalogEventJpaRepository.class).findById(id).orElseThrow();
        assertThat(entity.getEventId()).isEqualTo(id);
        assertThat(entity.getNewVersion()).isEqualTo(7);
    }

    @Test
    void repeatedRegistrationKeepsOneUnchangedRecord() {
        String id = prefix + "repeat";
        port.registerProcessed(id, 7);
        port.registerProcessed(id, 7);
        assertThat(rowCount(id)).isEqualTo(1);
        assertThat(storedVersion(id)).isEqualTo(7L);
    }

    @Test
    void rejectsConflictingVersionAndKeepsOriginalRecord() {
        String id = prefix + "conflict";
        port.registerProcessed(id, 7);
        assertThatThrownBy(() -> port.registerProcessed(id, 8))
                .isInstanceOf(ConflictingCatalogEventException.class);
        assertThat(storedVersion(id)).isEqualTo(7L);
        port.registerProcessed(prefix + "after-conflict", 9);
        assertThat(storedVersion(prefix + "after-conflict")).isEqualTo(9L);
    }

    @Test
    void survivesClosingAndReopeningApplicationContext() {
        String id = prefix + "restart";
        try (var first = openContext()) {
            first.getBean(ProcessedCatalogEventPort.class).registerProcessed(id, 7);
        }
        try (var second = openContext()) {
            var restarted = second.getBean(ProcessedCatalogEventPort.class);
            assertThat(restarted.findProcessedVersion(id)).hasValue(7);
            restarted.registerProcessed(id, 7);
        }
        assertThat(rowCount(id)).isEqualTo(1);
    }

    @Test
    void registrationCommitsIndependentlyOfCallerRollback() {
        String id = prefix + "outer-rollback";
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            port.registerProcessed(id, 7);
            assertThat(storedVersion(id)).isEqualTo(7L);
            status.setRollbackOnly();
        });
        assertThat(storedVersion(id)).isEqualTo(7L);
    }

    @Test
    void commitFailurePropagatesAndRollsBackInsertedRecord() {
        String id = prefix + "commit-failure";
        // A deferred trigger rejects this fixture at COMMIT, after INSERT and SELECT executed.
        jdbc.execute("""
                CREATE FUNCTION c61_test_reject_commit() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.event_id LIKE 'c61-test-%-commit-failure' THEN
                        RAISE EXCEPTION 'Controlled processed-event commit failure';
                    END IF;
                    RETURN NEW;
                END $$
                """);
        try {
            jdbc.execute("""
                    CREATE CONSTRAINT TRIGGER c61_test_commit_failure
                    AFTER INSERT ON processed_catalog_events DEFERRABLE INITIALLY DEFERRED
                    FOR EACH ROW EXECUTE FUNCTION c61_test_reject_commit()
                    """);
            assertThatThrownBy(() -> port.registerProcessed(id, 7)).isInstanceOf(RuntimeException.class);
            assertThat(port.findProcessedVersion(id)).isEmpty();
            assertThat(rowCount(id)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS c61_test_commit_failure ON processed_catalog_events");
            jdbc.execute("DROP FUNCTION c61_test_reject_commit()");
        }
        port.registerProcessed(prefix + "after-rollback", 8);
        assertThat(storedVersion(prefix + "after-rollback")).isEqualTo(8L);
    }

    @Test
    void concurrentSameVersionRegistrationsBothSucceed() throws Exception {
        String id = prefix + "concurrent-same";
        assertThat(concurrentRegister(id, 7, 7)).containsExactlyInAnyOrder("success", "success");
        assertThat(rowCount(id)).isEqualTo(1);
        assertThat(storedVersion(id)).isEqualTo(7L);
    }

    @Test
    void concurrentDifferentVersionsHaveOneWinnerAndOneConflict() throws Exception {
        String id = prefix + "concurrent-conflict";
        assertThat(concurrentRegister(id, 7, 8)).containsExactlyInAnyOrder("success", "conflict");
        assertThat(rowCount(id)).isEqualTo(1);
        assertThat(storedVersion(id)).isIn(7L, 8L);
    }

    @Test
    void seesCompetingUncommittedInsertAfterWaitingForItsCommit() throws Exception {
        String id = prefix + "waiting";
        try (var connection = context.getBean(DataSource.class).getConnection();
             var executor = Executors.newSingleThreadExecutor()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(
                    "INSERT INTO processed_catalog_events(event_id, new_version) VALUES (?, 7)")) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
            var pending = executor.submit(() -> port.registerProcessed(id, 7));
            try {
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                boolean waiting;
                do {
                    waiting = Boolean.TRUE.equals(jdbc.queryForObject("""
                            SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                              WHERE datname = current_database() AND wait_event_type = 'Lock'
                                AND query LIKE '%ON CONFLICT (event_id) DO NOTHING%')
                            """, Boolean.class));
                    if (!waiting) {
                        TimeUnit.MILLISECONDS.sleep(20);
                    }
                } while (!waiting && System.nanoTime() < deadline);
                assertThat(waiting).as("competing registration waits for the uncommitted unique key").isTrue();
            } finally {
                connection.commit();
            }
            pending.get(10, TimeUnit.SECONDS);
        }
        assertThat(rowCount(id)).isEqualTo(1);
        assertThat(port.findProcessedVersion(id)).hasValue(7);
    }

    private java.util.List<String> concurrentRegister(String id, long first, long second) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(registration(barrier, id, first));
            var two = executor.submit(registration(barrier, id, second));
            return java.util.List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        }
    }

    private Callable<String> registration(CyclicBarrier barrier, String id, long version) {
        return () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try {
                port.registerProcessed(id, version);
                return "success";
            } catch (ConflictingCatalogEventException exception) {
                return "conflict";
            }
        };
    }

    private Long storedVersion(String id) {
        return jdbc.queryForObject("SELECT new_version FROM processed_catalog_events WHERE event_id = ?",
                Long.class, id);
    }

    private int rowCount(String id) {
        return jdbc.queryForObject("SELECT count(*) FROM processed_catalog_events WHERE event_id = ?",
                Integer.class, id);
    }

    private ConfigurableApplicationContext openContext() {
        return new SpringApplicationBuilder(PersistenceTestConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .run("--spring.jpa.hibernate.ddl-auto=validate", "--spring.main.banner-mode=off");
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ProcessedCatalogEventEntity.class)
    @EnableJpaRepositories(basePackageClasses = ProcessedCatalogEventJpaRepository.class)
    @Import(JpaProcessedCatalogEventAdapter.class)
    static class PersistenceTestConfiguration {
    }
}
