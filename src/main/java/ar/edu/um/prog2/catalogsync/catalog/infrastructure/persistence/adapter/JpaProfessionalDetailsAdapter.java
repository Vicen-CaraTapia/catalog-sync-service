package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.LoadProfessionalDetailsPort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaProfessionalDetailsAdapter implements LoadProfessionalDetailsPort {

    private final ProfessionalJpaRepository professionalRepository;
    private final ProfessionalCategoryJpaRepository categoryRepository;
    private final WeeklyScheduleJpaRepository scheduleRepository;

    public JpaProfessionalDetailsAdapter(
            ProfessionalJpaRepository professionalRepository,
            ProfessionalCategoryJpaRepository categoryRepository,
            WeeklyScheduleJpaRepository scheduleRepository
    ) {
        this.professionalRepository = Objects.requireNonNull(professionalRepository);
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Optional<ProfessionalDetails> findByProfessionalId(long professionalId) {
        return professionalRepository.findById(professionalId).map(professional -> {
            var category = categoryRepository.findById(professional.getCategoryId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Professional category is missing from the local catalog"
                    ));
            var schedules = scheduleRepository.findByProfessionalIdOrderByIdAsc(professionalId)
                    .stream()
                    .map(CatalogPersistenceMapper::toDomain)
                    .toList();

            return new ProfessionalDetails(
                    CatalogPersistenceMapper.toDomain(professional),
                    CatalogPersistenceMapper.toDomain(category),
                    schedules
            );
        });
    }
}
