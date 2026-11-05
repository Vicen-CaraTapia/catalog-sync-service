package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.decoder.CatalogUpdatedEventDecoder;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor.Operation;
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
	private final SynchronizationRetryExecutor retryExecutor;

	public CatalogUpdatedListener(
			ObjectMapper objectMapper,
			CatalogUpdateNotificationUseCase useCase,
			SynchronizationRetryExecutor retryExecutor
	) {
		this.decoder = new CatalogUpdatedEventDecoder(objectMapper);
		this.useCase = Objects.requireNonNull(useCase);
		this.retryExecutor = Objects.requireNonNull(retryExecutor);
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

		retryExecutor.execute(Operation.CATALOG_NOTIFICATION,
				() -> useCase.onCatalogUpdated(event.eventId(), event.newVersion()));
		acknowledgment.acknowledge();
	}
}
