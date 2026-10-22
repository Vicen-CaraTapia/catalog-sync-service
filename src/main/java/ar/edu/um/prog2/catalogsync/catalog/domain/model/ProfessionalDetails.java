package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import java.util.List;
import java.util.Objects;

public record ProfessionalDetails(
        Professional professional,
        ProfessionalCategory category,
        List<WeeklySchedule> weeklySchedules
) {
    public ProfessionalDetails {
        Objects.requireNonNull(professional, "professional must be present");
        Objects.requireNonNull(category, "category must be present");
        weeklySchedules = List.copyOf(
                Objects.requireNonNull(weeklySchedules, "weeklySchedules must be present")
        );
    }
}
