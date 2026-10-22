package ar.edu.um.prog2.catalogsync.catalog.domain.ports.in;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;

public interface GetProfessionalDetailsUseCase {

    ProfessionalDetails getById(long professionalId);
}
