package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ProfessionalSearchPageResponseTest {

    @Test
    void preservesPageMetadataAndMakesContentImmutable() {
        List<ProfessionalSummaryResponse> content = new ArrayList<>(List.of(
                new ProfessionalSummaryResponse(1L, 2L, "Ana", "Gomez", true)
        ));

        ProfessionalSearchPageResponse response = new ProfessionalSearchPageResponse(content, 1, 10, 11, 2);
        content.clear();

        assertThat(response.content()).containsExactly(
                new ProfessionalSummaryResponse(1L, 2L, "Ana", "Gomez", true)
        );
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> response.content().clear());
    }

    @Test
    void rejectsNullContent() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ProfessionalSearchPageResponse(null, 0, 20, 0, 0));
    }

    @Test
    void rejectsNullContentElements() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProfessionalSearchPageResponse(
                        java.util.Arrays.asList((ProfessionalSummaryResponse) null),
                        0,
                        20,
                        0,
                        0
                ));
    }
}
