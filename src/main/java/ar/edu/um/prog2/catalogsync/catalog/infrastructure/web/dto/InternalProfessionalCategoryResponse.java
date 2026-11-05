package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.util.Objects;

public record InternalProfessionalCategoryResponse(
        Long id,
        String name,
        boolean enabled
) {
    public InternalProfessionalCategoryResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(name, "name must be present");
    }
}
