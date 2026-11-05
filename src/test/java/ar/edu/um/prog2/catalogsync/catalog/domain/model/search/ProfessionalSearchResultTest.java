package ar.edu.um.prog2.catalogsync.catalog.domain.model.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessionalSearchResultTest {

    @Test
    void permitsAnEmptyPageBeyondTheLastPage() {
        ProfessionalSearchResult result = new ProfessionalSearchResult(List.of(), 4, 20, 21, 2);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void copiesContentSoItCannotBeChangedAfterConstruction() {
        List<Professional> professionals = new ArrayList<>();
        professionals.add(professional());

        ProfessionalSearchResult result = new ProfessionalSearchResult(professionals, 0, 20, 1, 1);
        professionals.clear();

        assertThat(result.content()).hasSize(1);
        assertThatThrownBy(() -> result.content().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidPaginationMetadata() {
        assertThatThrownBy(() -> new ProfessionalSearchResult(List.of(), -1, 20, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchResult(List.of(), 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchResult(List.of(), 0, 101, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchResult(List.of(), 0, 20, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchResult(List.of(), 0, 20, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullElementsAndContentLargerThanPageSize() {
        assertThatThrownBy(() -> new ProfessionalSearchResult(new ArrayList<>(List.of(professional(), professional())), 0, 1, 2, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchResult(new ArrayList<>(java.util.Arrays.asList(professional(), null)), 0, 20, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Professional professional() {
        Instant timestamp = Instant.parse("2026-10-20T00:00:00Z");
        return new Professional(1L, 2L, "Ana", "Gomez", true, timestamp, timestamp);
    }
}
