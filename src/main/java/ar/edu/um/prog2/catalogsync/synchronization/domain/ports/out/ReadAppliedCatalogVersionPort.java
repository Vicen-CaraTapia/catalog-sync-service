package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out;

/** Reads the version committed for the local catalog replica. */
public interface ReadAppliedCatalogVersionPort {

    /**
     * Returns the applied version, or {@code null} if no catalog version has been applied yet.
     * A missing synchronization-state row is an integrity failure, not an uninitialized catalog.
     */
    Long readAppliedVersion();
}
