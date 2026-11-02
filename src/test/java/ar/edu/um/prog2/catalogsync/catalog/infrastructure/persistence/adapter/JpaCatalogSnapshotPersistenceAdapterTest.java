package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.StaleCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class JpaCatalogSnapshotPersistenceAdapterTest {

    @Mock
    private ProfessionalCategoryJpaRepository professionalCategoryRepository;

    @Mock
    private ProfessionalJpaRepository professionalRepository;

    @Mock
    private WeeklyScheduleJpaRepository weeklyScheduleRepository;

    @Mock
    private CatalogSyncStateRepository catalogSyncStateRepository;

    private JpaCatalogSnapshotPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCatalogSnapshotPersistenceAdapter(
                professionalCategoryRepository,
                professionalRepository,
                weeklyScheduleRepository,
                catalogSyncStateRepository
        );
    }

    @Test
    void nullAppliedVersionMeansCatalogIsUninitialized() {
        when(catalogSyncStateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.of(new CatalogSyncStateEntity(null)));

        assertThat(adapter.isInitialized()).isFalse();
    }

    @Test
    void anyAppliedVersionMeansCatalogIsInitialized() {
        when(catalogSyncStateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.of(new CatalogSyncStateEntity(0L)));

        assertThat(adapter.isInitialized()).isTrue();
    }

    @Test
    void missingSingletonRowIsAnIntegrityFailure() {
        when(catalogSyncStateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(adapter::isInitialized)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("catalog sync state is missing");
    }

    @Test
    void rejectsStaleSnapshotBeforeTouchingCatalogEntities() {
        var state = new CatalogSyncStateEntity(8L);
        when(catalogSyncStateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.of(state));

        assertThatThrownBy(() -> adapter.apply(emptySnapshot(7)))
                .isInstanceOf(StaleCatalogSnapshotException.class)
                .hasMessage("Catalog snapshot is older than the locally applied version");

        verifyNoInteractions(professionalCategoryRepository, professionalRepository, weeklyScheduleRepository);
        assertThat(state.getAppliedVersion()).isEqualTo(8L);
        verify(catalogSyncStateRepository, never()).flush();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {6, 7})
    void locksBeforeReconciliationAndUpdatesTheSameStateEntity(Long previousVersion) {
        var state = new CatalogSyncStateEntity(previousVersion);
        when(catalogSyncStateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.of(state));

        adapter.apply(emptySnapshot(7));

        var order = inOrder(catalogSyncStateRepository, professionalCategoryRepository);
        order.verify(catalogSyncStateRepository).findByStateId(CatalogSyncStateEntity.SINGLETON_ID);
        order.verify(professionalCategoryRepository).saveAll(List.of());
        assertThat(state.getAppliedVersion()).isEqualTo(7L);
        verify(catalogSyncStateRepository, never()).findById(CatalogSyncStateEntity.SINGLETON_ID);
        verify(catalogSyncStateRepository).flush();
    }

    @Test
    void missingLockedStateFailsBeforeReconciliation() {
        when(catalogSyncStateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.apply(emptySnapshot(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("catalog sync state is missing");

        verifyNoInteractions(professionalCategoryRepository, professionalRepository, weeklyScheduleRepository);
    }

    private static CatalogSnapshot emptySnapshot(long version) {
        return new CatalogSnapshot(version, Instant.EPOCH, List.of(), List.of(), List.of());
    }
}
