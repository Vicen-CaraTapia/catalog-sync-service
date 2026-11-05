package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.util.List;
import java.util.Objects;

public record InternalProfessionalDetailsResponse(
        Long id,
        String firstName,
        String lastName,
        boolean enabled,
        InternalProfessionalCategoryResponse category,
        List<WeeklyScheduleDetailResponse> weeklySchedules
) {
    public InternalProfessionalDetailsResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(firstName, "firstName must be present");
        Objects.requireNonNull(lastName, "lastName must be present");
        Objects.requireNonNull(category, "category must be present");
        weeklySchedules = List.copyOf(
                Objects.requireNonNull(weeklySchedules, "weeklySchedules must be present")
        );
    }
}
