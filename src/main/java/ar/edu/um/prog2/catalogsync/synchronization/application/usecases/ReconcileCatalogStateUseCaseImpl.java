package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import ar.edu.um.prog2.catalogsync.synchronization.application.service.CatalogVersionSynchronizationService;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ReconcileCatalogStateUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Coordinates version comparison without a transaction around Redis or the synchronization flow. */
@Service
public final class ReconcileCatalogStateUseCaseImpl implements ReconcileCatalogStateUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReconcileCatalogStateUseCaseImpl.class);

    private final ReadAppliedCatalogVersionPort appliedVersionPort;
    private final CatalogVersionSynchronizationService synchronization;

    public ReconcileCatalogStateUseCaseImpl(
            ReadAppliedCatalogVersionPort appliedVersionPort,
            CatalogVersionSynchronizationService synchronization
    ) {
        this.appliedVersionPort = Objects.requireNonNull(appliedVersionPort);
        this.synchronization = Objects.requireNonNull(synchronization);
    }

    @Override
    public void reconcile() {
        Long localVersion = appliedVersionPort.readAppliedVersion();
        if (localVersion == null) {
            throw new IllegalStateException("The local catalog has not been initialized");
        }

        var metadata = synchronization.readPublishedMetadata();
        if (localVersion > metadata.currentVersion()) {
            LOG.warn("Local catalog version is ahead of the observed Redis version; local state preserved.");
            return;
        }
        if (localVersion < metadata.currentVersion()) {
            synchronization.synchronizeTo(metadata.currentVersion());
        }
    }
}
