package ar.edu.um.prog2.catalogsync.catalog.application.exception;

public final class ProfessionalNotFoundException extends RuntimeException {
    public ProfessionalNotFoundException(long professionalId) {
        super("Professional not found: " + professionalId);
    }
}
