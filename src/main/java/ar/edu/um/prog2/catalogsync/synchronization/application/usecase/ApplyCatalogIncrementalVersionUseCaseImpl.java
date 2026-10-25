package ar.edu.um.prog2.catalogsync.synchronization.application.usecase;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.CatalogIncrementalPersistencePort;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplyCatalogIncrementalVersionUseCaseImpl implements ApplyCatalogIncrementalVersionUseCase {

    private final CatalogIncrementalPersistencePort persistence;

    public ApplyCatalogIncrementalVersionUseCaseImpl(CatalogIncrementalPersistencePort persistence) {
        this.persistence = Objects.requireNonNull(persistence);
    }

    @Override
    @Transactional
    public void apply(CatalogIncrementalChanges changes) {
        Objects.requireNonNull(changes);
        Long appliedVersion = persistence.readAppliedVersion();
        if (appliedVersion == null) {
            throw new IllegalStateException("The local catalog has not been initialized");
        }
        if (changes.requestedChangeVersion() != Math.addExact(appliedVersion, 1L)) {
            throw new IllegalStateException("Catalog change version is not the next local version");
        }
        persistence.applyAndAdvanceVersion(changes);
    }
}
