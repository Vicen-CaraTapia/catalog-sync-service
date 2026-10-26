package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

class CatalogUpdatedListenerTest {

	private final CatalogUpdateNotificationUseCase useCase = mock(CatalogUpdateNotificationUseCase.class);
	private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
	private final CatalogUpdatedListener listener = new CatalogUpdatedListener(
			new ObjectMapper(), useCase
	);

	@Test
	void acknowledgesOnlyAfterTheUseCaseCompletes() {
		listener.onMessage(record("7", validJson()), acknowledgment);

		var order = inOrder(useCase, acknowledgment);
		order.verify(useCase).onCatalogUpdated("catalog-event-7-4f3b", 7L);
		order.verify(acknowledgment).acknowledge();
		order.verifyNoMoreInteractions();
	}

	@Test
	void rejectsMissingMessageKeyWithoutDelegatingOrAcknowledging() {
		assertThatThrownBy(() -> listener.onMessage(record(null, validJson()), acknowledgment))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class);

		verifyNoInteractions(useCase, acknowledgment);
	}

	@Test
	void rejectsMismatchedMessageKeyWithoutDelegatingOrAcknowledging() {
		assertThatThrownBy(() -> listener.onMessage(record("8", validJson()), acknowledgment))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class);

		verifyNoInteractions(useCase, acknowledgment);
	}

	@Test
	void propagatesInvalidJsonWithoutDelegatingOrAcknowledging() {
		assertThatThrownBy(() -> listener.onMessage(record("7", "{not-json"), acknowledgment))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class);

		verifyNoInteractions(useCase, acknowledgment);
	}

	@Test
	void propagatesUseCaseFailureWithoutAcknowledging() {
		var failure = new IllegalStateException("Test processing failure");
		doThrow(failure).when(useCase).onCatalogUpdated("catalog-event-7-4f3b", 7L);

		assertThatThrownBy(() -> listener.onMessage(record("7", validJson()), acknowledgment))
				.isSameAs(failure);

		verifyNoInteractions(acknowledgment);
	}

	private static ConsumerRecord<String, String> record(String key, String value) {
		return new ConsumerRecord<>("test-catalog-topic", 0, 0L, key, value);
	}

	private static String validJson() {
		return """
				{
				  "eventId": "catalog-event-7-4f3b",
				  "eventType": "CatalogUpdated",
				  "newVersion": 7,
				  "occurredAt": "2026-09-10T13:44:59Z",
				  "source": "MANUAL",
				  "schemaVersion": 1
				}
				""";
	}
}
