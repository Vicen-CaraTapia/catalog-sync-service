package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.RecoverCatalogFromSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public final class CatalogUpdateNotificationUseCaseImpl implements CatalogUpdateNotificationUseCase {

    private final ReadAppliedCatalogVersionPort appliedVersionPort;
    private final ReadCatalogIncrementalChangesPort changesPort;
    private final ApplyCatalogIncrementalVersionUseCase applyVersion;
    private final RecoverCatalogFromSnapshotUseCase recoverSnapshot;
    private final ProcessedCatalogEventPort processedEvents;

    public CatalogUpdateNotificationUseCaseImpl(
            ReadAppliedCatalogVersionPort appliedVersionPort,
            ReadCatalogIncrementalChangesPort changesPort,
            ApplyCatalogIncrementalVersionUseCase applyVersion,
            RecoverCatalogFromSnapshotUseCase recoverSnapshot,
            ProcessedCatalogEventPort processedEvents
    ) {
        this.appliedVersionPort = Objects.requireNonNull(appliedVersionPort);
        this.changesPort = Objects.requireNonNull(changesPort);
        this.applyVersion = Objects.requireNonNull(applyVersion);
        this.recoverSnapshot = Objects.requireNonNull(recoverSnapshot);
        this.processedEvents = Objects.requireNonNull(processedEvents);
    }

    @Override
    public void onCatalogUpdated(String eventId, long newVersion) {
        Objects.requireNonNull(eventId);
        var processedVersion = processedEvents.findProcessedVersion(eventId);
        if (processedVersion.isPresent()) {
            if (processedVersion.getAsLong() != newVersion) {
                throw new ConflictingCatalogEventException();
            }
            return;
        }

        synchronize(newVersion);
        // C6.1 registration commits independently, only after synchronization succeeds.
        // Store the notified version even if recovery applied a newer snapshot.
        processedEvents.registerProcessed(eventId, newVersion);
    }

    private void synchronize(long newVersion) {
        Long localVersion = appliedVersionPort.readAppliedVersion();
        if (localVersion == null) {
            throw new IllegalStateException("The local catalog has not been initialized");
        }
        if (localVersion > newVersion) {
            return;
        }

        var metadata = checkedMetadata(newVersion);
        if (localVersion < metadata.oldestAvailableVersion() || metadata.currentVersion() > newVersion) {
            recoverSnapshot.recover(metadata.currentVersion());
            return;
        }
        if (localVersion == newVersion) {
            return;
        }

        long version = Math.addExact(localVersion, 1L);
        while (true) {
            var changes = changesPort.readChanges(version);
            if (changes.isEmpty()) {
                recoverSnapshot.recover(metadata.currentVersion());
                return;
            }
            if (changes.get().requestedChangeVersion() != version) {
                throw new IllegalStateException("Catalog changes version differs from the requested version");
            }
            applyVersion.apply(changes.get());
            if (version == newVersion) {
                break;
            }
            version = Math.addExact(version, 1L);
        }

        var finalMetadata = checkedMetadata(newVersion);
        if (finalMetadata.currentVersion() > newVersion) {
            recoverSnapshot.recover(finalMetadata.currentVersion());
        }
    }

    private CatalogSyncMetadata checkedMetadata(long newVersion) {
        var metadata = changesPort.readMetadata();
        if (metadata == null || metadata.oldestAvailableVersion() > metadata.currentVersion()) {
            throw new IllegalStateException("Redis catalog metadata is inconsistent");
        }
        if (metadata.currentVersion() < newVersion) {
            throw new IllegalStateException("Redis catalog version is behind the notified version");
        }
        return metadata;
    }
}
