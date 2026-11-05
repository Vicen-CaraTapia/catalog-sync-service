package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.SearchProfessionalsUseCase;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessionalSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessionalSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchProfessionalsUseCase searchProfessionalsUseCase;

    @Test
    void returnsTheRequestedPageAndDelegatesNormalizedCriteria() throws Exception {
        given(searchProfessionalsUseCase.search(any())).willReturn(new ProfessionalSearchResult(
                List.of(
                        professional(12L, 3L, "Ana", "Gomez", true),
                        professional(15L, 3L, "Luis", "Perez", false)
                ),
                1,
                2,
                5,
                3
        ));

        mockMvc.perform(get("/api/professionals")
                        .param("name", "  Ana  ")
                        .param("categoryId", "3")
                        .param("enabled", "false")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.content[0].categoryId").value(3))
                .andExpect(jsonPath("$.content[0].firstName").value("Ana"))
                .andExpect(jsonPath("$.content[0].lastName").value("Gomez"))
                .andExpect(jsonPath("$.content[0].enabled").value(true))
                .andExpect(jsonPath("$.content[1].enabled").value(false))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist());

        verify(searchProfessionalsUseCase).search(
                new ProfessionalSearchCriteria("Ana", 3L, false, null, 1, 2)
        );
    }

    @Test
    void usesDefaultPaginationAndTreatsBlankNameAsNoFilter() throws Exception {
        given(searchProfessionalsUseCase.search(any())).willReturn(new ProfessionalSearchResult(
                List.of(), 0, 20, 0, 0
        ));

        mockMvc.perform(get("/api/professionals").param("name", "  \t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(searchProfessionalsUseCase).search(
                new ProfessionalSearchCriteria(null, null, null, null, 0, 20)
        );
    }

    @Test
    void passesAvailabilityDateWithOtherFiltersAndReturnsAnEmptyPage() throws Exception {
        given(searchProfessionalsUseCase.search(any())).willReturn(new ProfessionalSearchResult(
                List.of(), 0, 20, 0, 0
        ));

        mockMvc.perform(get("/api/professionals")
                        .param("name", "  Ana  ")
                        .param("categoryId", "3")
                        .param("enabled", "true")
                        .param("availableOn", "2026-10-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(searchProfessionalsUseCase).search(
                new ProfessionalSearchCriteria("Ana", 3L, true, LocalDate.of(2026, 10, 26), 0, 20)
        );
    }

    @Test
    void rejectsInvalidAvailabilityDateBeforeCallingTheUseCase() throws Exception {
        mockMvc.perform(get("/api/professionals").param("availableOn", "26-10-2026"))
                .andExpect(status().isBadRequest());

        verify(searchProfessionalsUseCase, never()).search(any());
    }

    @Test
    void rejectsInvalidPaginationBeforeCallingTheUseCase() throws Exception {
        mockMvc.perform(get("/api/professionals").param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/professionals").param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/professionals").param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(searchProfessionalsUseCase, never()).search(any());
    }

    private static Professional professional(
            long id,
            long categoryId,
            String firstName,
            String lastName,
            boolean enabled
    ) {
        return new Professional(
                id,
                categoryId,
                firstName,
                lastName,
                enabled,
                Instant.parse("2026-10-22T00:00:00Z"),
                Instant.parse("2026-10-22T01:00:00Z")
        );
    }
}
