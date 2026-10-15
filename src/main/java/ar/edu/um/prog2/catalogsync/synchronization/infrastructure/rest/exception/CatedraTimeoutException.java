package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception;

public final class CatedraTimeoutException extends CatedraRestException {

	public CatedraTimeoutException(Throwable cause) {
		super("Catedra request timed out", cause);
	}
}
