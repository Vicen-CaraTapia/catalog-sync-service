package ar.edu.um.prog2.catalogsync.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.SearchProfessionalsPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchProfessionalsUseCaseImplTest {

    @Test
    void delegatesCriteriaAndReturnsThePortResult() {
        SearchProfessionalsPort searchPort = mock(SearchProfessionalsPort.class);
        SearchProfessionalsUseCaseImpl service = new SearchProfessionalsUseCaseImpl(searchPort);
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria("Ana", 42L, false, null, 0, 20);
        ProfessionalSearchResult expected = new ProfessionalSearchResult(List.of(), 0, 20, 0, 0);
        when(searchPort.search(criteria)).thenReturn(expected);

        ProfessionalSearchResult actual = service.search(criteria);

        assertThat(actual).isSameAs(expected);
        verify(searchPort).search(criteria);
    }

    @Test
    void rejectsNullCriteriaWithoutCallingThePort() {
        SearchProfessionalsPort searchPort = mock(SearchProfessionalsPort.class);
        SearchProfessionalsUseCaseImpl service = new SearchProfessionalsUseCaseImpl(searchPort);

        assertThatThrownBy(() -> service.search(null)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(searchPort);
    }
}
