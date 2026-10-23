package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

class CatalogUpdatedListenerTest {

	private final ObjectProvider<CatalogUpdateNotificationUseCase> useCaseProvider = mockProvider();
	private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
	private final CatalogUpdatedListener listener = new CatalogUpdatedListener(
			new ObjectMapper(), useCaseProvider
	);

	@Test
	void delegatesValidMessageWithMatchingKeyWithoutAcknowledging() {
		var useCase = mock(CatalogUpdateNotificationUseCase.class);
		when(useCaseProvider.getIfAvailable()).thenReturn(useCase);

		listener.onMessage(record("7", validJson()), acknowledgment);

		verify(useCase).onCatalogUpdated("catalog-event-7-4f3b", 7L);
		verifyNoInteractions(acknowledgment);
	}

	@Test
	void rejectsMissingMessageKeyWithoutDelegatingOrAcknowledging() {
		assertThatThrownBy(() -> listener.onMessage(record(null, validJson()), acknowledgment))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class);

		verifyNoInteractions(useCaseProvider, acknowledgment);
	}

	@Test
	void rejectsMismatchedMessageKeyWithoutDelegatingOrAcknowledging() {
		assertThatThrownBy(() -> listener.onMessage(record("8", validJson()), acknowledgment))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class);

		verifyNoInteractions(useCaseProvider, acknowledgment);
	}

	@Test
	void propagatesInvalidJsonWithoutDelegatingOrAcknowledging() {
		assertThatThrownBy(() -> listener.onMessage(record("7", "{not-json"), acknowledgment))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class);

		verifyNoInteractions(useCaseProvider, acknowledgment);
	}

	@Test
	void failsExplicitlyWhenUseCaseImplementationIsMissing() {
		assertThatThrownBy(() -> listener.onMessage(record("7", validJson()), acknowledgment))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Catalog update notification use case is not configured.");

		verifyNoInteractions(acknowledgment);
	}

	@Test
	void propagatesUseCaseFailureWithoutAcknowledging() {
		var useCase = mock(CatalogUpdateNotificationUseCase.class);
		var failure = new IllegalStateException("Test processing failure");
		when(useCaseProvider.getIfAvailable()).thenReturn(useCase);
		org.mockito.Mockito.doThrow(failure)
				.when(useCase).onCatalogUpdated("catalog-event-7-4f3b", 7L);

		assertThatThrownBy(() -> listener.onMessage(record("7", validJson()), acknowledgment))
				.isSameAs(failure);

		verifyNoInteractions(acknowledgment);
	}

	private static ConsumerRecord<String, String> record(String key, String value) {
		return new ConsumerRecord<>("test-catalog-topic", 0, 0L, key, value);
	}

	@SuppressWarnings("unchecked")
	private static ObjectProvider<CatalogUpdateNotificationUseCase> mockProvider() {
		return mock(ObjectProvider.class);
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
