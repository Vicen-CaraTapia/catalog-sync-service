package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in;

public interface ReconcileCatalogStateUseCase {

    /** Compares the initialized local catalog with published metadata without requiring a Kafka event. */
    void reconcile();
}
