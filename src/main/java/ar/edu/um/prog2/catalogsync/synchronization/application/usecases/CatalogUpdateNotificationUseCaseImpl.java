package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.application.service.CatalogVersionSynchronizationService;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public final class CatalogUpdateNotificationUseCaseImpl implements CatalogUpdateNotificationUseCase {

    private final CatalogVersionSynchronizationService synchronization;
    private final ProcessedCatalogEventPort processedEvents;

    public CatalogUpdateNotificationUseCaseImpl(
            CatalogVersionSynchronizationService synchronization,
            ProcessedCatalogEventPort processedEvents
    ) {
        this.synchronization = Objects.requireNonNull(synchronization);
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

        synchronization.synchronizeTo(newVersion);
        // C6.1 registration commits independently, only after synchronization succeeds.
        // Store the notified version even if recovery applied a newer snapshot.
        processedEvents.registerProcessed(eventId, newVersion);
    }

}
