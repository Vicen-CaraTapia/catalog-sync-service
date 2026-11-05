package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.SearchProfessionalsPort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public final class JpaProfessionalSearchAdapter implements SearchProfessionalsPort {

    private final ProfessionalJpaRepository repository;

    public JpaProfessionalSearchAdapter(ProfessionalJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public ProfessionalSearchResult search(ProfessionalSearchCriteria criteria) {
        Objects.requireNonNull(criteria);

        PageRequest pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by("lastName", "firstName", "id")
        );

        Page<ProfessionalEntity> page = repository.findAll(
                specification(criteria),
                pageable
        );

        return new ProfessionalSearchResult(
                page.getContent().stream()
                        .map(CatalogPersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Specification<ProfessionalEntity> specification(
            ProfessionalSearchCriteria criteria
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.name() != null) {
                String pattern = literalContainsPattern(criteria.name());

                predicates.add(builder.or(
                        builder.like(
                                builder.lower(root.get("firstName")),
                                pattern,
                                '!'
                        ),
                        builder.like(
                                builder.lower(root.get("lastName")),
                                pattern,
                                '!'
                        )
                ));
            }

            if (criteria.categoryId() != null) {
                predicates.add(builder.equal(
                        root.get("categoryId"),
                        criteria.categoryId()
                ));
            }

            if (criteria.enabled() != null) {
                predicates.add(builder.equal(
                        root.get("enabled"),
                        criteria.enabled()
                ));
            }

            if (criteria.availableOn() != null) {
                var matchingSchedule = query.subquery(Long.class);
                var schedule = matchingSchedule.from(WeeklyScheduleEntity.class);
                matchingSchedule.select(schedule.get("id")).where(
                        builder.equal(schedule.get("professionalId"), root.get("id")),
                        builder.isTrue(schedule.get("enabled")),
                        builder.equal(schedule.get("dayOfWeek"), criteria.availableOn().getDayOfWeek())
                );
                predicates.add(builder.exists(matchingSchedule));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String literalContainsPattern(String name) {
        String escaped = name.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }
}
