package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in;

public interface RecoverCatalogFromSnapshotUseCase {

    /**
     * Applies a complete snapshot covering at least the required version.
     * Returns the snapshot version after successful application and commit.
     */
    long recover(long minimumRequiredVersion);
}
