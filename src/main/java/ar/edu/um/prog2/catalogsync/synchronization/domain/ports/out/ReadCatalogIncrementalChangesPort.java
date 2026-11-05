package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out;

import java.util.Optional;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;

public interface ReadCatalogIncrementalChangesPort {

    CatalogSyncMetadata readMetadata();

    Optional<CatalogIncrementalChanges> readChanges(long version);
}
