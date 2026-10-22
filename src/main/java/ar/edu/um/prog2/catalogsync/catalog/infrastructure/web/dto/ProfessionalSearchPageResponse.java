package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.util.List;
import java.util.Objects;

/**
 * HTTP page returned by the internal professional-search operation.
 */
public record ProfessionalSearchPageResponse(
        List<ProfessionalSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public ProfessionalSearchPageResponse {
        Objects.requireNonNull(content, "content must be present");
        if (content.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("content must not contain null elements");
        }
        content = List.copyOf(content);
    }
}
