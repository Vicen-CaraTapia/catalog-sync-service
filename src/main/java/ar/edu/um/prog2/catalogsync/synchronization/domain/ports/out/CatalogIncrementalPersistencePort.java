package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;

/** Persistence needed to apply one version within a single transaction. */
public interface CatalogIncrementalPersistencePort {

    /** Returns the applied version while serializing updates to the local catalog state. */
    Long readAppliedVersion();

    void applyAndAdvanceVersion(CatalogIncrementalChanges changes);
}
