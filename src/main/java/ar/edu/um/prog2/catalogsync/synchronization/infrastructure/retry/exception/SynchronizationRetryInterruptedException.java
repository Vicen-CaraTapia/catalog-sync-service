package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.exception;

public class SynchronizationRetryInterruptedException extends RuntimeException {

    public SynchronizationRetryInterruptedException(InterruptedException cause) {
        super("Synchronization retry execution was interrupted", cause);
    }
}
