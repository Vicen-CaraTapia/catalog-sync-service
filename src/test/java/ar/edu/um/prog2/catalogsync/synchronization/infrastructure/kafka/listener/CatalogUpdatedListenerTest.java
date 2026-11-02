package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.TransientSynchronizationFailureClassifier;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

class CatalogUpdatedListenerTest {

	private final CatalogUpdateNotificationUseCase useCase = mock(CatalogUpdateNotificationUseCase.class);
	private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
	private final ArrayList<Duration> delays = new ArrayList<>();
	private final CatalogUpdatedListener listener = new CatalogUpdatedListener(
			new ObjectMapper(), useCase, retryExecutor()
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

	@Test
	void retriesNotificationAndAcknowledgesOnlyAfterSuccessfulRetry() {
		doThrow(new CatedraRedisUnavailableException(new SocketTimeoutException()))
				.doNothing().when(useCase).onCatalogUpdated("catalog-event-7-4f3b", 7L);

		listener.onMessage(record("7", validJson()), acknowledgment);

		verify(useCase, org.mockito.Mockito.times(2))
				.onCatalogUpdated("catalog-event-7-4f3b", 7L);
		verify(acknowledgment).acknowledge();
		org.assertj.core.api.Assertions.assertThat(delays).containsExactly(Duration.ofSeconds(1));
	}

	@Test
	void doesNotAcknowledgeWhenAllNotificationAttemptsFail() {
		var failure = new CatedraRedisUnavailableException(new SocketTimeoutException());
		org.mockito.Mockito.doThrow(failure).when(useCase).onCatalogUpdated("catalog-event-7-4f3b", 7L);

		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> listener.onMessage(record("7", validJson()), acknowledgment))
				.isSameAs(failure);

		verify(useCase, org.mockito.Mockito.times(3)).onCatalogUpdated("catalog-event-7-4f3b", 7L);
		verifyNoInteractions(acknowledgment);
		org.assertj.core.api.Assertions.assertThat(delays)
				.containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
	}

	private SynchronizationRetryExecutor retryExecutor() {
		return new SynchronizationRetryExecutor(
				new CatalogSynchronizationRetryProperties(3, Duration.ofSeconds(1), Duration.ofSeconds(2)),
				new TransientSynchronizationFailureClassifier(), delays::add);
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
