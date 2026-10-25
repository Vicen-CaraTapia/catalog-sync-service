package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public final class CatalogUpdateNotificationUseCaseImpl implements CatalogUpdateNotificationUseCase {

    private final ReadAppliedCatalogVersionPort appliedVersionPort;
    private final ReadCatalogIncrementalChangesPort changesPort;
    private final ApplyCatalogIncrementalVersionUseCase applyVersion;

    public CatalogUpdateNotificationUseCaseImpl(
            ReadAppliedCatalogVersionPort appliedVersionPort,
            ReadCatalogIncrementalChangesPort changesPort,
            ApplyCatalogIncrementalVersionUseCase applyVersion
    ) {
        this.appliedVersionPort = Objects.requireNonNull(appliedVersionPort);
        this.changesPort = Objects.requireNonNull(changesPort);
        this.applyVersion = Objects.requireNonNull(applyVersion);
    }

    @Override
    public void onCatalogUpdated(String eventId, long newVersion) {
        Long localVersion = appliedVersionPort.readAppliedVersion();
        if (localVersion == null) {
            throw new IllegalStateException("The local catalog has not been initialized");
        }
        if (localVersion > newVersion) {
            return;
        }

        requireRedisVersion(newVersion);
        if (localVersion == newVersion) {
            return;
        }

        long version = Math.addExact(localVersion, 1L);
        while (true) {
            var changes = changesPort.readChanges(version)
                    .orElseThrow(() -> new IllegalStateException("Required catalog changes are unavailable"));
            if (changes.requestedChangeVersion() != version) {
                throw new IllegalStateException("Catalog changes version differs from the requested version");
            }
            applyVersion.apply(changes);
            if (version == newVersion) {
                break;
            }
            version = Math.addExact(version, 1L);
        }

        requireRedisVersion(newVersion);
    }

    private void requireRedisVersion(long newVersion) {
        if (changesPort.readMetadata().currentVersion() != newVersion) {
            throw new IllegalStateException("Redis catalog version differs from the notified version");
        }
    }
}
