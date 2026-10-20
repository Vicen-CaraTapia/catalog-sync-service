package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CatalogSnapshotResponse(
        Long snapshotVersion,
        Instant generatedAt,
        List<ProfessionalCategoryResponse> professionalCategories,
        List<ProfessionalResponse> professionals,
        List<WeeklyScheduleResponse> weeklySchedules
) {

    public CatalogSnapshotResponse {
        requirePresent(snapshotVersion, "snapshotVersion");
        requirePresent(generatedAt, "generatedAt");
        professionalCategories = immutableWithoutNulls(
                professionalCategories,
                "professionalCategories"
        );
        professionals = immutableWithoutNulls(professionals, "professionals");
        weeklySchedules = immutableWithoutNulls(weeklySchedules, "weeklySchedules");
    }

    private static <T> T requirePresent(T value, String field) {
        return Objects.requireNonNull(value, field + " must be present");
    }

    private static <T> List<T> immutableWithoutNulls(List<T> values, String field) {
        requirePresent(values, field);
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(field + " must not contain null elements");
        }
        return List.copyOf(values);
    }
}
