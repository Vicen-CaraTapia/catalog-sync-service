package ar.edu.um.prog2.catalogsync.catalog.application.usecases;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.GetProfessionalDetailsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.LoadProfessionalDetailsPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public final class GetProfessionalDetailsUseCaseImpl implements GetProfessionalDetailsUseCase {

    private final LoadProfessionalDetailsPort detailsPort;

    public GetProfessionalDetailsUseCaseImpl(LoadProfessionalDetailsPort detailsPort) {
        this.detailsPort = Objects.requireNonNull(detailsPort);
    }

    @Override
    public ProfessionalDetails getById(long professionalId) {
        return detailsPort.findByProfessionalId(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
    }
}
