package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.decoder.CatalogUpdatedEventDecoder;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class CatalogUpdatedListener {

	private final CatalogUpdatedEventDecoder decoder;
	private final ObjectProvider<CatalogUpdateNotificationUseCase> useCaseProvider;

	public CatalogUpdatedListener(
			ObjectMapper objectMapper,
			ObjectProvider<CatalogUpdateNotificationUseCase> useCaseProvider
	) {
		this.decoder = new CatalogUpdatedEventDecoder(objectMapper);
		this.useCaseProvider = useCaseProvider;
	}

	@KafkaListener(
			id = "catedraCatalogUpdatedListener",
			idIsGroup = false,
			topics = "${catedra.kafka.catalog-topic}",
			containerFactory = "catedraCatalogListenerContainerFactory",
			autoStartup = "false"
	)
	public void onMessage(
			ConsumerRecord<String, String> record,
			Acknowledgment acknowledgment
	) {
		var event = decoder.decode(record.value());
		if (record.key() == null || !record.key().equals(Long.toString(event.newVersion()))) {
			throw new InvalidCatalogUpdatedEventException();
		}

		var useCase = useCaseProvider.getIfAvailable();
		if (useCase == null) {
			throw new IllegalStateException("Catalog update notification use case is not configured.");
		}
		useCase.onCatalogUpdated(event.eventId(), event.newVersion());
	}
}
