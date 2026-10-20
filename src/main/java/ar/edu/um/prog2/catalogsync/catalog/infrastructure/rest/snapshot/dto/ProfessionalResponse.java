package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto;

import java.time.Instant;
import java.util.Objects;

public record ProfessionalResponse(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public ProfessionalResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(categoryId, "categoryId must be present");
        Objects.requireNonNull(firstName, "firstName must be present");
        Objects.requireNonNull(lastName, "lastName must be present");
        Objects.requireNonNull(enabled, "enabled must be present");
        Objects.requireNonNull(createdAt, "createdAt must be present");
        Objects.requireNonNull(updatedAt, "updatedAt must be present");
    }
}
