package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogSnapshotPersistencePort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public final class JpaCatalogSnapshotPersistenceAdapter implements CatalogSnapshotPersistencePort {

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
    public void apply(CatalogSnapshot snapshot) {
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

        weeklyScheduleRepository.deleteAll(absentEntities(
                weeklyScheduleRepository.findAll(),
                snapshot.weeklySchedules().stream().map(weeklySchedule -> weeklySchedule.id()).collect(java.util.stream.Collectors.toSet()),
                weeklySchedule -> weeklySchedule.getId()
        ));
        weeklyScheduleRepository.flush();

        professionalRepository.deleteAll(absentEntities(
                professionalRepository.findAll(),
                snapshot.professionals().stream().map(professional -> professional.id()).collect(java.util.stream.Collectors.toSet()),
                professional -> professional.getId()
        ));
        professionalRepository.flush();

        professionalCategoryRepository.deleteAll(absentEntities(
                professionalCategoryRepository.findAll(),
                snapshot.professionalCategories().stream().map(category -> category.id()).collect(java.util.stream.Collectors.toSet()),
                category -> category.getId()
        ));
        professionalCategoryRepository.flush();

        CatalogSyncStateEntity syncState = catalogSyncStateRepository
                .findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("catalog sync state is missing"));
        syncState.updateAppliedVersion(snapshot.version());
        catalogSyncStateRepository.flush();
    }

    private static <T> List<T> absentEntities(
            List<T> existingEntities,
            Set<Long> incomingIds,
            Function<T, Long> idExtractor
    ) {
        return existingEntities.stream()
                .filter(entity -> !incomingIds.contains(idExtractor.apply(entity)))
                .toList();
    }
}
