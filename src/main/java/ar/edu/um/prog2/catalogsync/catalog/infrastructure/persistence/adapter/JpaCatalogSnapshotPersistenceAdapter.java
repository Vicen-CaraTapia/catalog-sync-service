package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import static java.util.stream.Collectors.toSet;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.StaleCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogInitializationStatePort;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogSnapshotPersistencePort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;
import org.springframework.stereotype.Component;

@Component
public final class JpaCatalogSnapshotPersistenceAdapter
        implements CatalogSnapshotPersistencePort, CatalogInitializationStatePort {

    private final ProfessionalCategoryJpaRepository professionalCategoryRepository;
    private final ProfessionalJpaRepository professionalRepository;
    private final WeeklyScheduleJpaRepository weeklyScheduleRepository;
    private final CatalogSyncStateRepository catalogSyncStateRepository;

    public JpaCatalogSnapshotPersistenceAdapter(
            ProfessionalCategoryJpaRepository professionalCategoryRepository,
            ProfessionalJpaRepository professionalRepository,
            WeeklyScheduleJpaRepository weeklyScheduleRepository,
            CatalogSyncStateRepository catalogSyncStateRepository
    ) {
        this.professionalCategoryRepository = professionalCategoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.catalogSyncStateRepository = catalogSyncStateRepository;
    }

    @Override
    public boolean isInitialized() {
        return syncState().getAppliedVersion() != null;
    }

    @Override
    public void apply(CatalogSnapshot snapshot) {
        // Called inside ApplyCatalogSnapshotUseCase's transaction. Share the incremental lock.
        CatalogSyncStateEntity lockedState = catalogSyncStateRepository
                .findByStateId(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("catalog sync state is missing"));
        Long appliedVersion = lockedState.getAppliedVersion();
        if (appliedVersion != null && appliedVersion > snapshot.version()) {
            throw new StaleCatalogSnapshotException();
        }

        saveSnapshotEntities(snapshot);
        deleteAbsentEntities(snapshot);

        lockedState.updateAppliedVersion(snapshot.version());
        catalogSyncStateRepository.flush();
    }

    private void saveSnapshotEntities(CatalogSnapshot snapshot) {
        professionalCategoryRepository.saveAll(
                snapshot.professionalCategories().stream()
                        .map(CatalogPersistenceMapper::toEntity)
                        .toList()
        );
        professionalCategoryRepository.flush();

        professionalRepository.saveAll(
                snapshot.professionals().stream()
                        .map(CatalogPersistenceMapper::toEntity)
                        .toList()
        );
        professionalRepository.flush();

        weeklyScheduleRepository.saveAll(
                snapshot.weeklySchedules().stream()
                        .map(CatalogPersistenceMapper::toEntity)
                        .toList()
        );
        weeklyScheduleRepository.flush();
    }

    private void deleteAbsentEntities(CatalogSnapshot snapshot) {
        weeklyScheduleRepository.deleteAll(absentEntities(
                weeklyScheduleRepository.findAll(),
                snapshot.weeklySchedules().stream().map(WeeklySchedule::id).collect(toSet()),
                WeeklyScheduleEntity::getId
        ));
        weeklyScheduleRepository.flush();

        professionalRepository.deleteAll(absentEntities(
                professionalRepository.findAll(),
                snapshot.professionals().stream().map(Professional::id).collect(toSet()),
                ProfessionalEntity::getId
        ));
        professionalRepository.flush();

        professionalCategoryRepository.deleteAll(absentEntities(
                professionalCategoryRepository.findAll(),
                snapshot.professionalCategories().stream().map(ProfessionalCategory::id).collect(toSet()),
                ProfessionalCategoryEntity::getId
        ));
        professionalCategoryRepository.flush();
    }

    private CatalogSyncStateEntity syncState() {
        return catalogSyncStateRepository
                .findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("catalog sync state is missing"));
    }

    private static <T> List<T> absentEntities(
            List<T> existingEntities,
            Set<Long> incomingIds,
            ToLongFunction<T> idExtractor
    ) {
        return existingEntities.stream()
                .filter(entity -> !incomingIds.contains(idExtractor.applyAsLong(entity)))
                .toList();
    }
}
