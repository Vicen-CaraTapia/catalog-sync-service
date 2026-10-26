package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.GetProfessionalDetailsUseCase;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessionalDetailsController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessionalDetailsControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-21T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetProfessionalDetailsUseCase useCase;

    @Test
    void returnsProfessionalCategoryAndEnabledAndDisabledSchedules() throws Exception {
        given(useCase.getById(10L)).willReturn(new ProfessionalDetails(
                new Professional(10L, 20L, "Ana", "Gomez", false, CREATED_AT, UPDATED_AT),
                new ProfessionalCategory(20L, "Salud", null, true, CREATED_AT, UPDATED_AT),
                List.of(
                        new WeeklySchedule(30L, 10L, DayOfWeek.MONDAY,
                                LocalTime.of(9, 0), LocalTime.of(12, 0), 30,
                                false, CREATED_AT, UPDATED_AT),
                        new WeeklySchedule(31L, 10L, DayOfWeek.TUESDAY,
                                LocalTime.of(10, 0), LocalTime.of(13, 0), 20,
                                true, CREATED_AT, UPDATED_AT)
                )
        ));

        mockMvc.perform(get("/api/professionals/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.category.id").value(20))
                .andExpect(jsonPath("$.category.name").value("Salud"))
                .andExpect(jsonPath("$.category.enabled").value(true))
                .andExpect(jsonPath("$.weeklySchedules[0].id").value(30))
                .andExpect(jsonPath("$.weeklySchedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.weeklySchedules[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.weeklySchedules[0].enabled").value(false))
                .andExpect(jsonPath("$.weeklySchedules[1].enabled").value(true))
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        verify(useCase).getById(10L);
    }

    @Test
    void returnsAnEmptyScheduleListWhenNoneAreStored() throws Exception {
        given(useCase.getById(10L)).willReturn(new ProfessionalDetails(
                new Professional(10L, 20L, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT),
                new ProfessionalCategory(20L, "Salud", null, true, CREATED_AT, UPDATED_AT),
                List.of()
        ));

        mockMvc.perform(get("/api/professionals/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules").isEmpty());
    }

    @Test
    void returnsControlledNotFoundForMissingProfessional() throws Exception {
        given(useCase.getById(999L)).willThrow(new ProfessionalNotFoundException(999L));

        mockMvc.perform(get("/api/professionals/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Professional not found."));

        verify(useCase).getById(999L);
    }
}
