package ar.edu.um.prog2.catalogsync.catalog.domain.ports.out;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;

public interface SearchProfessionalsPort {

    ProfessionalSearchResult search(ProfessionalSearchCriteria criteria);
}
