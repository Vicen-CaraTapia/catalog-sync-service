package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto.ProfessionalSearchPageResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto.ProfessionalSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/professionals")
public class ProfessionalSearchController {

    private final SearchProfessionalsUseCase searchProfessionalsUseCase;

    public ProfessionalSearchController(SearchProfessionalsUseCase searchProfessionalsUseCase) {
        this.searchProfessionalsUseCase = Objects.requireNonNull(searchProfessionalsUseCase);
    }

    @GetMapping
    public ProfessionalSearchPageResponse search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableOn,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(ProfessionalSearchCriteria.MAX_SIZE) int size
    ) {
        var criteria = new ProfessionalSearchCriteria(name, categoryId, enabled, availableOn, page, size);
        return toResponse(searchProfessionalsUseCase.search(criteria));
    }

    private static ProfessionalSearchPageResponse toResponse(ProfessionalSearchResult result) {
        List<ProfessionalSummaryResponse> content = result.content().stream()
                .map(ProfessionalSearchController::toResponse)
                .toList();

        return new ProfessionalSearchPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private static ProfessionalSummaryResponse toResponse(Professional professional) {
        return new ProfessionalSummaryResponse(
                professional.id(),
                professional.categoryId(),
                professional.firstName(),
                professional.lastName(),
                professional.enabled()
        );
    }
}
