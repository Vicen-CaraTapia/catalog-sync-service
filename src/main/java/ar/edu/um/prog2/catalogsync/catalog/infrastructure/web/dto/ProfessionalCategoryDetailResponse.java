package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.util.Objects;

public record ProfessionalCategoryDetailResponse(
        Long id,
        String name,
        String description,
        boolean enabled
) {
    public ProfessionalCategoryDetailResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(name, "name must be present");
    }
}
