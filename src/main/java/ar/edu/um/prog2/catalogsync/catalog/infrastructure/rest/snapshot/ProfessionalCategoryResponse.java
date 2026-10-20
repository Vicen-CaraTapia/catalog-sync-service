package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot;

import java.time.Instant;
import java.util.Objects;

public record ProfessionalCategoryResponse(
        Long id,
        String name,
        String description,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public ProfessionalCategoryResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(name, "name must be present");
        Objects.requireNonNull(enabled, "enabled must be present");
        Objects.requireNonNull(createdAt, "createdAt must be present");
        Objects.requireNonNull(updatedAt, "updatedAt must be present");
    }
}
