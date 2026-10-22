package ar.edu.um.prog2.catalogsync.catalog.domain.model.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProfessionalSearchCriteriaTest {

    @Test
    void trimsNameWithoutChangingOtherSearchCharacters() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                "  A_na%!  ", 42L, false, 0, 20
        );

        assertThat(criteria.name()).isEqualTo("A_na%!");
        assertThat(criteria.categoryId()).isEqualTo(42L);
        assertThat(criteria.enabled()).isFalse();
    }

    @Test
    void treatsBlankOrAbsentNameAsNoFilter() {
        assertThat(new ProfessionalSearchCriteria(" \t\n ", null, null, 0, 20).name()).isNull();
        assertThat(new ProfessionalSearchCriteria(null, null, null, 0, 20).name()).isNull();
    }

    @Test
    void acceptsZeroBasedPageAndMaximumSize() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(null, null, true, 3, 100);

        assertThat(criteria.page()).isEqualTo(3);
        assertThat(criteria.size()).isEqualTo(ProfessionalSearchCriteria.MAX_SIZE);
    }

    @Test
    void rejectsNegativePageAndSizesOutsideTheAllowedRange() {
        assertThatThrownBy(() -> new ProfessionalSearchCriteria(null, null, null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchCriteria(null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfessionalSearchCriteria(null, null, null, 0, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
