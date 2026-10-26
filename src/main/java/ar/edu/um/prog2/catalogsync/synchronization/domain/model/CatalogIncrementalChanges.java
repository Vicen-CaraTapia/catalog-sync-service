package ar.edu.um.prog2.catalogsync.synchronization.domain.model;

import java.util.List;
import java.util.Objects;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;

/**
 * Entities affected by a requested change version, read from the current Redis hashes.
 * Their state is not a historical snapshot of that version.
 */
public record CatalogIncrementalChanges(
        long requestedChangeVersion,
        List<ProfessionalCategory> currentProfessionalCategories,
        List<Professional> currentProfessionals,
        List<WeeklySchedule> currentWeeklySchedules
) {

    public CatalogIncrementalChanges {
        currentProfessionalCategories = immutableWithoutNulls(
                currentProfessionalCategories, "currentProfessionalCategories"
        );
        currentProfessionals = immutableWithoutNulls(currentProfessionals, "currentProfessionals");
        currentWeeklySchedules = immutableWithoutNulls(currentWeeklySchedules, "currentWeeklySchedules");
    }

    private static <T> List<T> immutableWithoutNulls(List<T> values, String field) {
        Objects.requireNonNull(values, field + " must be present");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(field + " must not contain null elements");
        }
        return List.copyOf(values);
    }
}
