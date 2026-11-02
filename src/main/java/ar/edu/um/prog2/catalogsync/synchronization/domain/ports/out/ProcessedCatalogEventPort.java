package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out;

import java.util.OptionalLong;

/** Durable records of notifications whose processing has already completed successfully. */
public interface ProcessedCatalogEventPort {

    OptionalLong findProcessedVersion(String eventId);

    /**
     * Commits the registration before returning. Repeating the same ID and version succeeds;
     * an ID associated with another version is rejected. This does not reserve processing.
     */
    void registerProcessed(String eventId, long newVersion);
}
