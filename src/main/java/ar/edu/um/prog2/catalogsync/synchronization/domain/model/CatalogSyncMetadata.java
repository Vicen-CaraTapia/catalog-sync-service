package ar.edu.um.prog2.catalogsync.synchronization.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CatalogSyncMetadata(
        long currentVersion,
        long oldestAvailableVersion,
        Instant publishedAt
) {

    public CatalogSyncMetadata {
        Objects.requireNonNull(publishedAt, "publishedAt must be present");
    }
}
