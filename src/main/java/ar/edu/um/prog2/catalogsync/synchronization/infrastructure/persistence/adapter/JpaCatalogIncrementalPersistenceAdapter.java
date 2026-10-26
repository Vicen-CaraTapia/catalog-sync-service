package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.CatalogIncrementalPersistencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class JpaCatalogIncrementalPersistenceAdapter implements CatalogIncrementalPersistencePort {

    private final CatalogSyncStateRepository stateRepository;
    private final ProfessionalCategoryJpaRepository categoryRepository;
    private final ProfessionalJpaRepository professionalRepository;
    private final WeeklyScheduleJpaRepository scheduleRepository;

    public JpaCatalogIncrementalPersistenceAdapter(
            CatalogSyncStateRepository stateRepository,
            ProfessionalCategoryJpaRepository categoryRepository,
            ProfessionalJpaRepository professionalRepository,
            WeeklyScheduleJpaRepository scheduleRepository
    ) {
        this.stateRepository = stateRepository;
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public Long readAppliedVersion() {
        return lockedState().getAppliedVersion();
    }

    @Override
    public void applyAndAdvanceVersion(CatalogIncrementalChanges changes) {
        categoryRepository.saveAll(changes.currentProfessionalCategories().stream()
                .map(CatalogPersistenceMapper::toEntity).toList());
        categoryRepository.flush();

        professionalRepository.saveAll(changes.currentProfessionals().stream()
                .map(CatalogPersistenceMapper::toEntity).toList());
        professionalRepository.flush();

        scheduleRepository.saveAll(changes.currentWeeklySchedules().stream()
                .map(CatalogPersistenceMapper::toEntity).toList());
        scheduleRepository.flush();

        lockedState().updateAppliedVersion(changes.requestedChangeVersion());
        stateRepository.flush();
    }

    private CatalogSyncStateEntity lockedState() {
        return stateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("catalog sync state is missing"));
    }
}
