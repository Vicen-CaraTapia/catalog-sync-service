package ar.edu.um.prog2.catalogsync.synchronization.application.exception;

public class ConflictingCatalogEventException extends RuntimeException {

    public ConflictingCatalogEventException() {
        super("Catalog event ID is already associated with a different version");
    }
}
