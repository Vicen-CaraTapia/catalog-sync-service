package ar.edu.um.prog2.catalogsync.synchronization.application.exception;

public class InsufficientCatalogSnapshotVersionException extends RuntimeException {

    public InsufficientCatalogSnapshotVersionException() {
        super("Catalog snapshot does not cover the minimum required version");
    }
}
