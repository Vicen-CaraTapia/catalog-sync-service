package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.GetProfessionalDetailsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto.InternalProfessionalCategoryResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto.InternalProfessionalDetailsResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto.WeeklyScheduleDetailResponse;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/professionals")
public class InternalProfessionalDetailsController {

    private final GetProfessionalDetailsUseCase useCase;

    public InternalProfessionalDetailsController(GetProfessionalDetailsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase);
    }

    @GetMapping("/{id}")
    public InternalProfessionalDetailsResponse getById(@PathVariable long id) {
        return toResponse(useCase.getById(id));
    }

    private static InternalProfessionalDetailsResponse toResponse(ProfessionalDetails details) {
        var professional = details.professional();
        var category = details.category();

        return new InternalProfessionalDetailsResponse(
                professional.id(),
                professional.firstName(),
                professional.lastName(),
                professional.enabled(),
                new InternalProfessionalCategoryResponse(
                        category.id(),
                        category.name(),
                        category.enabled()
                ),
                details.weeklySchedules().stream()
                        .map(schedule -> new WeeklyScheduleDetailResponse(
                                schedule.id(),
                                schedule.dayOfWeek(),
                                schedule.startTime(),
                                schedule.endTime(),
                                schedule.slotDurationMinutes(),
                                schedule.enabled()
                        ))
                        .toList()
        );
    }
}
