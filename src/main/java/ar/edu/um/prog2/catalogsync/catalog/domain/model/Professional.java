package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;

public record Professional(
        long id,
        long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public Professional {
        Objects.requireNonNull(firstName, "firstName must be present");
        Objects.requireNonNull(lastName, "lastName must be present");
        Objects.requireNonNull(createdAt, "createdAt must be present");
        Objects.requireNonNull(updatedAt, "updatedAt must be present");
    }
}
