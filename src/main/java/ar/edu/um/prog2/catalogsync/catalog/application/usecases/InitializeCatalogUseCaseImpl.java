package ar.edu.um.prog2.catalogsync.catalog.application.usecases;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogInitializationStatePort;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public final class InitializeCatalogUseCaseImpl implements InitializeCatalogUseCase {

    private final CatalogInitializationStatePort initializationStatePort;
    private final FetchCatalogSnapshotPort fetchCatalogSnapshotPort;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    public InitializeCatalogUseCaseImpl(
            CatalogInitializationStatePort initializationStatePort,
            FetchCatalogSnapshotPort fetchCatalogSnapshotPort,
            ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase
    ) {
        this.initializationStatePort = Objects.requireNonNull(initializationStatePort);
        this.fetchCatalogSnapshotPort = Objects.requireNonNull(fetchCatalogSnapshotPort);
        this.applyCatalogSnapshotUseCase = Objects.requireNonNull(applyCatalogSnapshotUseCase);
    }

    @Override
    public void initializeIfNeeded() {
        if (initializationStatePort.isInitialized()) {
            return;
        }

        applyCatalogSnapshotUseCase.apply(fetchCatalogSnapshotPort.fetchSnapshot());
    }
}
