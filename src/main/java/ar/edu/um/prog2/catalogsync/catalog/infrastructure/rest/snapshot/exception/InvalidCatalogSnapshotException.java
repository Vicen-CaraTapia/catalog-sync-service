package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.exception;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraRestException;

public final class InvalidCatalogSnapshotException extends CatedraRestException {

    public InvalidCatalogSnapshotException(Throwable cause) {
        super("Catedra catalog snapshot response is invalid", cause);
    }
}
