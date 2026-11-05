package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.GetProfessionalDetailsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.exception.ProfessionalDetailsExceptionHandler;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Isolated HTTP contract tests; production filter configuration is not changed. */
class InternalProfessionalDetailsControllerTest {
    private final GetProfessionalDetailsUseCase useCase = mock(GetProfessionalDetailsUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new InternalProfessionalDetailsController(useCase))
            .setControllerAdvice(new ProfessionalDetailsExceptionHandler()).build();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsExactlyTheApprovedFieldsIncludingDisabledSchedules(boolean enabled) throws Exception {
        when(useCase.getById(10)).thenReturn(details(enabled, List.of(
                new WeeklySchedule(30L, 10L, DayOfWeek.MONDAY, LocalTime.of(9, 0),
                        LocalTime.of(12, 0), 30, true, Instant.EPOCH, Instant.EPOCH),
                new WeeklySchedule(31L, 10L, DayOfWeek.TUESDAY, LocalTime.of(10, 0),
                        LocalTime.of(13, 0), 20, false, Instant.EPOCH, Instant.EPOCH))));
        mvc.perform(get("/internal/api/professionals/10"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {"id":10,"firstName":"Ana","lastName":"Gomez","enabled":%s,
                     "category":{"id":20,"name":"Salud","enabled":false},
                     "weeklySchedules":[
                       {"id":30,"dayOfWeek":"MONDAY","startTime":"09:00:00","endTime":"12:00:00","slotDurationMinutes":30,"enabled":true},
                       {"id":31,"dayOfWeek":"TUESDAY","startTime":"10:00:00","endTime":"13:00:00","slotDurationMinutes":20,"enabled":false}]}
                    """.formatted(enabled), JsonCompareMode.STRICT));
        verify(useCase).getById(10);
    }

    @Test
    void returnsEmptySchedules() throws Exception {
        when(useCase.getById(10)).thenReturn(details(true, List.of()));
        mvc.perform(get("/internal/api/professionals/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(longs = {999, 0, -1})
    void unknownNumericIdReturnsProblemJson(long id) throws Exception {
        when(useCase.getById(id)).thenThrow(new ProfessionalNotFoundException(id));
        mvc.perform(get("/internal/api/professionals/{id}", id)).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                    {"title":"Not Found","status":404,
                     "detail":"Professional not found.","instance":"/internal/api/professionals/%d"}
                    """.formatted(id), JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "9223372036854775808"})
    void rejectsUnparseableIdsWithoutCallingTheUseCase(String id) throws Exception {
        mvc.perform(get("/internal/api/professionals/{id}", id)).andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    private static ProfessionalDetails details(boolean enabled, List<WeeklySchedule> schedules) {
        return new ProfessionalDetails(
                new Professional(10L, 20L, "Ana", "Gomez", enabled, Instant.EPOCH, Instant.EPOCH),
                new ProfessionalCategory(20L, "Salud", "Must not leak", false, Instant.EPOCH, Instant.EPOCH),
                schedules);
    }
}
