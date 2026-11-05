package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.util.Objects;

/**
 * HTTP representation of a professional returned by the internal catalog API.
 */
public record ProfessionalSummaryResponse(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled
) {

    public ProfessionalSummaryResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(categoryId, "categoryId must be present");
        Objects.requireNonNull(firstName, "firstName must be present");
        Objects.requireNonNull(lastName, "lastName must be present");
    }
}
