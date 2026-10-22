package ar.edu.um.prog2.catalogsync.catalog.application.usecase;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.SearchProfessionalsPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public final class SearchProfessionalsService implements SearchProfessionalsUseCase {

    private final SearchProfessionalsPort searchPort;

    public SearchProfessionalsService(SearchProfessionalsPort searchPort) {
        this.searchPort = Objects.requireNonNull(searchPort);
    }

    @Override
    public ProfessionalSearchResult search(ProfessionalSearchCriteria criteria) {
        return searchPort.search(Objects.requireNonNull(criteria));
    }
}
