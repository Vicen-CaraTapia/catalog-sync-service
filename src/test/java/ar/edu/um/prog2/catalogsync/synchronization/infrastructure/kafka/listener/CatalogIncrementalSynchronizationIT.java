package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Exercises listener-to-database behavior without a Kafka broker or cátedra Redis. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CatalogIncrementalSynchronizationIT {

    private static final long CATEGORY_ID = -541_001L;
    private static final long PROFESSIONAL_ID = -542_001L;
    private static final long SCHEDULE_ID = -543_001L;
    private static final long INVALID_CATEGORY_ID = -549_999L;
    private static final Instant CREATED = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-09-02T00:00:00Z");

    @Autowired
    private CatalogUpdatedListener listener;

    @MockitoBean
    private ReadCatalogIncrementalChangesPort changesPort;

    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    private JdbcTemplate jdbc;
    private Long originalVersion;
    private boolean restoreRequired;

    @Autowired
    void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void seedCatalog() {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        if (database == null || !database.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException("Incremental flow tests require a dedicated test database");
        }
        originalVersion = appliedVersion();
        restoreRequired = true;
        removeFixtures();
        jdbc.update("UPDATE catalog_sync_state SET applied_version = 4 WHERE state_id = 1");
        jdbc.update("INSERT INTO professional_categories VALUES (?, ?, ?, ?, ?, ?)",
                CATEGORY_ID, "Original", null, true, CREATED.atOffset(ZoneOffset.UTC),
                CREATED.atOffset(ZoneOffset.UTC));
        jdbc.update("INSERT INTO professionals VALUES (?, ?, ?, ?, ?, ?, ?)",
                PROFESSIONAL_ID, CATEGORY_ID, "Ana", "Original", true,
                CREATED.atOffset(ZoneOffset.UTC), CREATED.atOffset(ZoneOffset.UTC));
        jdbc.update("INSERT INTO weekly_schedules VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                SCHEDULE_ID, PROFESSIONAL_ID, "MONDAY", LocalTime.of(9, 0),
                LocalTime.of(12, 0), 30, true,
                CREATED.atOffset(ZoneOffset.UTC), CREATED.atOffset(ZoneOffset.UTC));
    }

    @AfterEach
    void restoreCatalog() {
        if (restoreRequired) {
            removeFixtures();
            jdbc.update("UPDATE catalog_sync_state SET applied_version = ? WHERE state_id = 1", originalVersion);
        }
    }

    @Test
    void appliesEveryPendingVersionAndAcknowledgesAfterTheFinalCommit() {
        when(changesPort.readMetadata()).thenReturn(metadata(7), metadata(7));
        when(changesPort.readChanges(5)).thenReturn(Optional.of(changes(5,
                List.of(category("Updated", false)), List.of(), List.of())));
        when(changesPort.readChanges(6)).thenReturn(Optional.of(changes(6,
                List.of(), List.of(professional(CATEGORY_ID, "Beatriz", false)), List.of())));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(7,
                List.of(), List.of(), List.of(schedule(false)))));

        listener.onMessage(record(7), acknowledgment);

        assertThat(appliedVersion()).isEqualTo(7L);
        assertThat(jdbc.queryForObject("SELECT name FROM professional_categories WHERE id = ?",
                String.class, CATEGORY_ID)).isEqualTo("Updated");
        assertThat(jdbc.queryForObject("SELECT enabled FROM professional_categories WHERE id = ?",
                Boolean.class, CATEGORY_ID)).isFalse();
        assertThat(jdbc.queryForObject("SELECT first_name FROM professionals WHERE id = ?",
                String.class, PROFESSIONAL_ID)).isEqualTo("Beatriz");
        assertThat(jdbc.queryForObject("SELECT enabled FROM professionals WHERE id = ?",
                Boolean.class, PROFESSIONAL_ID)).isFalse();
        assertThat(jdbc.queryForObject("SELECT enabled FROM weekly_schedules WHERE id = ?",
                Boolean.class, SCHEDULE_ID)).isFalse();
        var order = inOrder(changesPort, acknowledgment);
        order.verify(changesPort).readMetadata();
        order.verify(changesPort).readChanges(5);
        order.verify(changesPort).readChanges(6);
        order.verify(changesPort).readChanges(7);
        order.verify(changesPort).readMetadata();
        order.verify(acknowledgment).acknowledge();
    }

    @Test
    void keepsEarlierCommittedVersionButDoesNotAcknowledgeWhenPostgresRejectsTheNext() {
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(5)).thenReturn(Optional.of(changes(5,
                List.of(category("Committed", false)), List.of(), List.of())));
        when(changesPort.readChanges(6)).thenReturn(Optional.of(changes(6,
                List.of(), List.of(professional(INVALID_CATEGORY_ID, "Rejected", false)), List.of())));

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(RuntimeException.class);

        assertThat(appliedVersion()).isEqualTo(5L);
        assertThat(jdbc.queryForObject("SELECT name FROM professional_categories WHERE id = ?",
                String.class, CATEGORY_ID)).isEqualTo("Committed");
        assertThat(jdbc.queryForObject("SELECT category_id FROM professionals WHERE id = ?",
                Long.class, PROFESSIONAL_ID)).isEqualTo(CATEGORY_ID);
        assertThat(jdbc.queryForObject("SELECT first_name FROM professionals WHERE id = ?",
                String.class, PROFESSIONAL_ID)).isEqualTo("Ana");
        verify(changesPort, never()).readChanges(7);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void doesNotAcknowledgeWhenRedisAdvancesAfterTheTargetVersionCommitted() {
        jdbc.update("UPDATE catalog_sync_state SET applied_version = 6 WHERE state_id = 1");
        when(changesPort.readMetadata()).thenReturn(metadata(7), metadata(8));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(7,
                List.of(category("Committed", false)), List.of(), List.of())));

        assertThatThrownBy(() -> listener.onMessage(record(7), acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis catalog version differs");

        assertThat(appliedVersion()).isEqualTo(7L);
        assertThat(jdbc.queryForObject("SELECT name FROM professional_categories WHERE id = ?",
                String.class, CATEGORY_ID)).isEqualTo("Committed");
        verify(changesPort).readChanges(7);
        verify(acknowledgment, never()).acknowledge();
    }

    private Long appliedVersion() {
        return jdbc.queryForObject("SELECT applied_version FROM catalog_sync_state WHERE state_id = 1", Long.class);
    }

    private void removeFixtures() {
        jdbc.update("DELETE FROM weekly_schedules WHERE id = ?", SCHEDULE_ID);
        jdbc.update("DELETE FROM professionals WHERE id = ?", PROFESSIONAL_ID);
        jdbc.update("DELETE FROM professional_categories WHERE id = ?", CATEGORY_ID);
    }

    private static CatalogSyncMetadata metadata(long version) {
        return new CatalogSyncMetadata(version, 1, UPDATED);
    }

    private static CatalogIncrementalChanges changes(long version,
            List<ProfessionalCategory> categories, List<Professional> professionals,
            List<WeeklySchedule> schedules) {
        return new CatalogIncrementalChanges(version, categories, professionals, schedules);
    }

    private static ProfessionalCategory category(String name, boolean enabled) {
        return new ProfessionalCategory(CATEGORY_ID, name, null, enabled, CREATED, UPDATED);
    }

    private static Professional professional(long categoryId, String firstName, boolean enabled) {
        return new Professional(PROFESSIONAL_ID, categoryId, firstName, "Original", enabled, CREATED, UPDATED);
    }

    private static WeeklySchedule schedule(boolean enabled) {
        return new WeeklySchedule(SCHEDULE_ID, PROFESSIONAL_ID, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30, enabled, CREATED, UPDATED);
    }

    private static ConsumerRecord<String, String> record(long version) {
        return new ConsumerRecord<>("test-catalog-topic", 0, 0L, Long.toString(version), """
                {"eventId":"catalog-event-7-4f3b","eventType":"CatalogUpdated","newVersion":7,
                 "occurredAt":"2026-09-10T13:44:59Z","source":"MANUAL","schemaVersion":1}
                """);
    }
}
