package ar.edu.um.prog2.catalogsync.catalog.domain.ports.in;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;

public interface SearchProfessionalsUseCase {

    ProfessionalSearchResult search(ProfessionalSearchCriteria criteria);
}
