package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
