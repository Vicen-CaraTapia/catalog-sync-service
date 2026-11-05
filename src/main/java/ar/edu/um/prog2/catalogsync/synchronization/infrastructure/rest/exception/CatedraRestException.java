package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception;

public abstract class CatedraRestException extends RuntimeException {

	public CatedraRestException(String message, Throwable cause) {
		super(message, cause);
	}
}
