package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;

public record ProfessionalCategory(
        long id,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public ProfessionalCategory {
        Objects.requireNonNull(name, "name must be present");
        Objects.requireNonNull(createdAt, "createdAt must be present");
        Objects.requireNonNull(updatedAt, "updatedAt must be present");
    }
}
