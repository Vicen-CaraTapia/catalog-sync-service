package ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in;

public interface CatalogUpdateNotificationUseCase {

	void onCatalogUpdated(String eventId, long newVersion);
}
