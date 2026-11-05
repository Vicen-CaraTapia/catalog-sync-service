package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception;

public final class CatedraTransportException extends CatedraRestException {

	public CatedraTransportException(Throwable cause) {
		super("Catedra transport request failed", cause);
	}
}
