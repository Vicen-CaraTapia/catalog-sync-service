package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception;

public final class InvalidCatalogUpdatedEventException extends RuntimeException {

	public InvalidCatalogUpdatedEventException() {
		super("Invalid CatalogUpdated event.");
	}
}
