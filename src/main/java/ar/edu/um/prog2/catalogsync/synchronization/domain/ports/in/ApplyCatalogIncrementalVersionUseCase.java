package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;

/** Applies exactly one catalog change version to the local replica. */
public interface ApplyCatalogIncrementalVersionUseCase {

    void apply(CatalogIncrementalChanges changes);
}
