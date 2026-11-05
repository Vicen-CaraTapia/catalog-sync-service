package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToLongFunction;

public record CatalogSnapshot(
        long version,
        Instant generatedAt,
        List<ProfessionalCategory> professionalCategories,
        List<Professional> professionals,
        List<WeeklySchedule> weeklySchedules
) {

    public CatalogSnapshot {
        Objects.requireNonNull(generatedAt, "generatedAt must be present");
        professionalCategories = immutableWithoutNulls(professionalCategories, "professionalCategories");
        professionals = immutableWithoutNulls(professionals, "professionals");
        weeklySchedules = immutableWithoutNulls(weeklySchedules, "weeklySchedules");

        Set<Long> categoryIds = uniqueIds(
                professionalCategories,
                ProfessionalCategory::id,
                "professional category"
        );
        Set<Long> professionalIds = uniqueIds(professionals, Professional::id, "professional");
        uniqueIds(weeklySchedules, WeeklySchedule::id, "weekly schedule");

        professionals.forEach(professional -> {
            if (!categoryIds.contains(professional.categoryId())) {
                throw new IllegalArgumentException(
                        "professional categoryId must reference an incoming professional category"
                );
            }
        });
        weeklySchedules.forEach(schedule -> {
            if (!professionalIds.contains(schedule.professionalId())) {
                throw new IllegalArgumentException(
                        "weekly schedule professionalId must reference an incoming professional"
                );
            }
        });
    }

    private static <T> List<T> immutableWithoutNulls(List<T> values, String field) {
        Objects.requireNonNull(values, field + " must be present");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(field + " must not contain null elements");
        }
        return List.copyOf(values);
    }

    private static <T> Set<Long> uniqueIds(
            List<T> values,
            ToLongFunction<T> idExtractor,
            String entityName
    ) {
        Set<Long> ids = new HashSet<>();
        for (T value : values) {
            if (!ids.add(idExtractor.applyAsLong(value))) {
                throw new IllegalArgumentException("duplicate " + entityName + " id");
            }
        }
        return ids;
    }
}
