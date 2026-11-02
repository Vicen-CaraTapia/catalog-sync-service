package ar.edu.um.prog2.catalogsync.catalog.application.exception;

public class StaleCatalogSnapshotException extends RuntimeException {

    public StaleCatalogSnapshotException() {
        super("Catalog snapshot is older than the locally applied version");
    }
}
