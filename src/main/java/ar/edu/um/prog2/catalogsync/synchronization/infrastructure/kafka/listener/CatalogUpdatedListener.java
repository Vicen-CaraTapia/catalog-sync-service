package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.decoder.CatalogUpdatedEventDecoder;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class CatalogUpdatedListener {

	public static final String LISTENER_ID = "catedraCatalogUpdatedListener";

	private final CatalogUpdatedEventDecoder decoder;
	private final CatalogUpdateNotificationUseCase useCase;

	public CatalogUpdatedListener(
			ObjectMapper objectMapper,
			CatalogUpdateNotificationUseCase useCase
	) {
		this.decoder = new CatalogUpdatedEventDecoder(objectMapper);
		this.useCase = Objects.requireNonNull(useCase);
	}

	@KafkaListener(
			id = LISTENER_ID,
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

		useCase.onCatalogUpdated(event.eventId(), event.newVersion());
		acknowledgment.acknowledge();
	}
}
