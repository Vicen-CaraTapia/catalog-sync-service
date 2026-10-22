package ar.edu.um.prog2.catalogsync.catalog.domain.ports.out;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;

import java.util.Optional;

public interface LoadProfessionalDetailsPort {

    Optional<ProfessionalDetails> findByProfessionalId(long professionalId);
}
