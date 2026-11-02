package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ReconcileCatalogStateUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real use cases, JPA adapters and commits. Invoked explicitly before adding the startup runner. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CatalogReconciliationIT {

    private static final long CATEGORY = 901;
    private static final long PROFESSIONAL = 915;
    private static final long SCHEDULE = 921;

    @Autowired
    private ReconcileCatalogStateUseCase reconcileCatalog;
    @Autowired
    private ApplyCatalogSnapshotUseCase applySnapshot;
    @Autowired
    private DataSource dataSource;
    @MockitoBean
    private ReadCatalogIncrementalChangesPort changesPort;
    @MockitoBean
    private FetchCatalogSnapshotPort fetchSnapshot;

    private JdbcTemplate jdbc;
    private Long eventCountBefore;
    private boolean cleanupRequired;

    @BeforeEach
    void seed() {
        jdbc = new JdbcTemplate(dataSource);
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        if (database == null || !database.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException("Reconciliation IT requires a dedicated test database");
        }
        cleanupRequired = true;
        clearCatalog();
        applySnapshot.apply(snapshot(7, "Before", true));
        eventCountBefore = eventCount();
    }

    @AfterEach
    void cleanup() {
        if (cleanupRequired) {
            try {
                assertThat(eventCount()).as("reconciliation must not invent processed Kafka events")
                        .isEqualTo(eventCountBefore);
            } finally {
                clearCatalog();
            }
        }
    }

    @Test
    void equalVersionsLeaveCatalogUnchangedWithoutApplyingOrFetchingAnything() {
        var before = state();
        when(changesPort.readMetadata()).thenReturn(metadata(7, 1));

        reconcileCatalog.reconcile();

        assertThat(state()).isEqualTo(before);
        verify(changesPort, never()).readChanges(8);
        verifyNoInteractions(fetchSnapshot);
    }

    @Test
    void catchesUpFromSevenToNineThroughRealPerVersionTransactions() {
        continuousHistory();

        reconcileCatalog.reconcile();

        assertIncrementalResult();
        verifyNoInteractions(fetchSnapshot);
    }

    @Test
    void localVersionBelowOldestAvailableVersionUsesExistingSnapshotRecovery() {
        when(changesPort.readMetadata()).thenReturn(metadata(9, 8));
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot(10, "Recovered", false));

        reconcileCatalog.reconcile();

        assertThat(appliedVersion()).isEqualTo(10L);
        assertThat(categoryName()).isEqualTo("Recovered");
        verify(changesPort, never()).readChanges(8);
        verify(fetchSnapshot).fetchSnapshot();
    }

    @Test
    void missingChangesetRecoversWithoutSkippingTheMissingVersion() {
        when(changesPort.readMetadata()).thenReturn(metadata(9, 1));
        when(changesPort.readChanges(8)).thenReturn(Optional.empty());
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot(9, "Recovered", false));

        reconcileCatalog.reconcile();

        assertThat(appliedVersion()).isEqualTo(9L);
        assertThat(categoryName()).isEqualTo("Recovered");
        verify(changesPort, never()).readChanges(9);
    }

    @Test
    void repeatedReconciliationDoesNotRepeatAlreadyCommittedChanges() {
        continuousHistory();
        reconcileCatalog.reconcile();
        var afterFirst = state();

        reconcileCatalog.reconcile();

        assertThat(state()).isEqualTo(afterFirst);
        verify(changesPort, times(1)).readChanges(8);
        verify(changesPort, times(1)).readChanges(9);
        verifyNoInteractions(fetchSnapshot);
    }

    @Test
    void localAheadOfObservedRedisMetadataIsPreservedWithoutDowngrade() {
        applySnapshot.apply(snapshot(9, "Ahead", true));
        var before = state();
        when(changesPort.readMetadata()).thenReturn(metadata(7, 1));

        reconcileCatalog.reconcile();

        assertThat(state()).isEqualTo(before);
        verifyNoInteractions(fetchSnapshot);
        verify(changesPort, never()).readChanges(8);
    }

    @Test
    void redisFailurePropagatesWithoutBeingInterpretedAsMissingHistory() {
        var before = state();
        var failure = new CatedraRedisUnavailableException(null);
        when(changesPort.readMetadata()).thenThrow(failure);

        assertThatThrownBy(reconcileCatalog::reconcile).isSameAs(failure);

        assertThat(state()).isEqualTo(before);
        verifyNoInteractions(fetchSnapshot);
    }

    @Test
    void metadataAdvanceAfterInitialComparisonIsObservedByTheSharedService() {
        when(changesPort.readMetadata()).thenReturn(metadata(9, 1), metadata(10, 1));
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot(10, "Newer observation", true));

        reconcileCatalog.reconcile();

        assertThat(appliedVersion()).isEqualTo(10L);
        assertThat(categoryName()).isEqualTo("Newer observation");
        verify(changesPort, never()).readChanges(8);
        verify(fetchSnapshot).fetchSnapshot();
    }

    private void continuousHistory() {
        when(changesPort.readMetadata()).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return metadata(9, 1);
        });
        when(changesPort.readChanges(8)).thenReturn(Optional.of(new CatalogIncrementalChanges(8,
                List.of(category("After eight")), List.of(), List.of())));
        when(changesPort.readChanges(9)).thenReturn(Optional.of(new CatalogIncrementalChanges(9,
                List.of(), List.of(professional(false)), List.of())));
    }

    private void assertIncrementalResult() {
        assertThat(appliedVersion()).isEqualTo(9L);
        assertThat(categoryName()).isEqualTo("After eight");
        assertThat(jdbc.queryForObject("SELECT enabled FROM professionals WHERE id = ?",
                Boolean.class, PROFESSIONAL)).isFalse();
    }

    private List<Object> state() {
        return List.of(appliedVersion(), jdbc.queryForList("SELECT * FROM professional_categories ORDER BY id"),
                jdbc.queryForList("SELECT * FROM professionals ORDER BY id"),
                jdbc.queryForList("SELECT * FROM weekly_schedules ORDER BY id"));
    }

    private Long appliedVersion() {
        return jdbc.queryForObject("SELECT applied_version FROM catalog_sync_state WHERE state_id = 1", Long.class);
    }

    private String categoryName() {
        return jdbc.queryForObject("SELECT name FROM professional_categories WHERE id = ?", String.class, CATEGORY);
    }

    private Long eventCount() {
        return jdbc.queryForObject("SELECT count(*) FROM processed_catalog_events", Long.class);
    }

    private void clearCatalog() {
        jdbc.update("DELETE FROM weekly_schedules");
        jdbc.update("DELETE FROM professionals");
        jdbc.update("DELETE FROM professional_categories");
        jdbc.update("UPDATE catalog_sync_state SET applied_version = NULL WHERE state_id = 1");
    }

    private static CatalogSyncMetadata metadata(long current, long oldest) {
        return new CatalogSyncMetadata(current, oldest, Instant.EPOCH);
    }

    private static CatalogSnapshot snapshot(long version, String name, boolean enabled) {
        return new CatalogSnapshot(version, Instant.EPOCH, List.of(category(name)),
                List.of(professional(enabled)), List.of(new WeeklySchedule(SCHEDULE, PROFESSIONAL,
                        DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true,
                        Instant.EPOCH, Instant.EPOCH)));
    }

    private static ProfessionalCategory category(String name) {
        return new ProfessionalCategory(CATEGORY, name, null, true, Instant.EPOCH, Instant.EPOCH);
    }

    private static Professional professional(boolean enabled) {
        return new Professional(PROFESSIONAL, CATEGORY, "Ana", "Example", enabled, Instant.EPOCH, Instant.EPOCH);
    }
}
