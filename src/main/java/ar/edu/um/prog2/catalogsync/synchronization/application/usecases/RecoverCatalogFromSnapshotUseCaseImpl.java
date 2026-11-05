package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.InsufficientCatalogSnapshotVersionException;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.RecoverCatalogFromSnapshotUseCase;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Fetches outside the database transaction; C3.3 owns transactional snapshot application. */
@Service
public final class RecoverCatalogFromSnapshotUseCaseImpl implements RecoverCatalogFromSnapshotUseCase {

    private final FetchCatalogSnapshotPort fetchSnapshot;
    private final ApplyCatalogSnapshotUseCase applySnapshot;

    public RecoverCatalogFromSnapshotUseCaseImpl(
            FetchCatalogSnapshotPort fetchSnapshot,
            ApplyCatalogSnapshotUseCase applySnapshot
    ) {
        this.fetchSnapshot = Objects.requireNonNull(fetchSnapshot);
        this.applySnapshot = Objects.requireNonNull(applySnapshot);
    }

    @Override
    public long recover(long minimumRequiredVersion) {
        var snapshot = Objects.requireNonNull(fetchSnapshot.fetchSnapshot(), "Catalog snapshot must be present");
        if (snapshot.version() < minimumRequiredVersion) {
            throw new InsufficientCatalogSnapshotVersionException();
        }

        // Invoke the injected use-case bean so its transactional proxy completes before returning.
        applySnapshot.apply(snapshot);
        return snapshot.version();
    }
}
