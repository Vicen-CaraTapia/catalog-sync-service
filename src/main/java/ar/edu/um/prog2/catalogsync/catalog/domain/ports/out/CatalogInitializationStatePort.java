package ar.edu.um.prog2.catalogsync.catalog.domain.ports.out;

public interface CatalogInitializationStatePort {

    /**
     * Returns whether a catalog version has been successfully applied.
     * A missing synchronization-state row is an integrity failure, not an uninitialized catalog.
     *
     * @throws IllegalStateException if the synchronization-state row is missing
     */
    boolean isInitialized();
}
