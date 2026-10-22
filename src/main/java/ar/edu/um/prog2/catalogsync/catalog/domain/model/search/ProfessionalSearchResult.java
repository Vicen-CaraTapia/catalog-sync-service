package ar.edu.um.prog2.catalogsync.catalog.domain.model.search;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import java.util.List;
import java.util.Objects;

public record ProfessionalSearchResult(
        List<Professional> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public ProfessionalSearchResult {
        Objects.requireNonNull(content, "content must be present");
        if (content.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("content must not contain null elements");
        }
        content = List.copyOf(content);

        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (size < 1 || size > ProfessionalSearchCriteria.MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + ProfessionalSearchCriteria.MAX_SIZE);
        }
        if (totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("pagination totals must be non-negative");
        }
        if (content.size() > size) {
            throw new IllegalArgumentException("content must not exceed page size");
        }
    }
}
