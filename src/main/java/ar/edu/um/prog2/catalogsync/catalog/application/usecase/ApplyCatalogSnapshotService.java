package ar.edu.um.prog2.catalogsync.catalog.application.usecase;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogSnapshotPersistencePort;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class ApplyCatalogSnapshotService implements ApplyCatalogSnapshotUseCase {

    private final CatalogSnapshotPersistencePort persistencePort;

    public ApplyCatalogSnapshotService(CatalogSnapshotPersistencePort persistencePort) {
        this.persistencePort = Objects.requireNonNull(persistencePort);
    }

    @Override
    @Transactional
    public void apply(CatalogSnapshot snapshot) {
        persistencePort.apply(Objects.requireNonNull(snapshot));
    }
}
