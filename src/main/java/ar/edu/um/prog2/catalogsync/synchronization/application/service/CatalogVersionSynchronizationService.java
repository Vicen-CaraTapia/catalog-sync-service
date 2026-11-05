package ar.edu.um.prog2.catalogsync.synchronization.application.service;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.RecoverCatalogFromSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Coordinates synchronization without opening a transaction around external reads. */
@Service
public final class CatalogVersionSynchronizationService {

    private final ReadAppliedCatalogVersionPort appliedVersionPort;
    private final ReadCatalogIncrementalChangesPort changesPort;
    private final ApplyCatalogIncrementalVersionUseCase applyVersion;
    private final RecoverCatalogFromSnapshotUseCase recoverSnapshot;

    public CatalogVersionSynchronizationService(
            ReadAppliedCatalogVersionPort appliedVersionPort,
            ReadCatalogIncrementalChangesPort changesPort,
            ApplyCatalogIncrementalVersionUseCase applyVersion,
            RecoverCatalogFromSnapshotUseCase recoverSnapshot
    ) {
        this.appliedVersionPort = Objects.requireNonNull(appliedVersionPort);
        this.changesPort = Objects.requireNonNull(changesPort);
        this.applyVersion = Objects.requireNonNull(applyVersion);
        this.recoverSnapshot = Objects.requireNonNull(recoverSnapshot);
    }

    public void synchronizeTo(long targetVersion) {
        Long localVersion = appliedVersionPort.readAppliedVersion();
        if (localVersion == null) {
            throw new IllegalStateException("The local catalog has not been initialized");
        }
        if (localVersion > targetVersion) {
            return;
        }

        var metadata = checkedMetadata(targetVersion);
        if (localVersion < metadata.oldestAvailableVersion() || metadata.currentVersion() > targetVersion) {
            recoverSnapshot.recover(metadata.currentVersion());
            return;
        }
        if (localVersion == targetVersion) {
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
            if (version == targetVersion) {
                break;
            }
            version = Math.addExact(version, 1L);
        }

        var finalMetadata = checkedMetadata(targetVersion);
        if (finalMetadata.currentVersion() > targetVersion) {
            recoverSnapshot.recover(finalMetadata.currentVersion());
        }
    }

    /** Reads and validates a fresh observation of the published synchronization metadata. */
    public CatalogSyncMetadata readPublishedMetadata() {
        var metadata = changesPort.readMetadata();
        if (metadata == null || metadata.oldestAvailableVersion() > metadata.currentVersion()) {
            throw new IllegalStateException("Redis catalog metadata is inconsistent");
        }
        return metadata;
    }

    private CatalogSyncMetadata checkedMetadata(long targetVersion) {
        var metadata = readPublishedMetadata();
        if (metadata.currentVersion() < targetVersion) {
            throw new IllegalStateException("Redis catalog version is behind the notified version");
        }
        return metadata;
    }
}
