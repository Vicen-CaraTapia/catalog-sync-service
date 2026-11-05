package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.CatedraKafkaConnectivityException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.CatedraKafkaConnectivityException.Reason;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.ConsumerFactory;

@ExtendWith(MockitoExtension.class)
class CatedraKafkaConnectivityVerifierTest {

	private static final String TOPIC = "test-catalog-topic";
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	@Mock
	private ConsumerFactory<String, String> consumerFactory;

	@Mock
	private Consumer<String, String> consumer;

	@Mock
	private CatedraIntegrationProperties properties;

	private CatedraKafkaConnectivityVerifier verifier;

	@BeforeEach
	void setUp() {
		when(consumerFactory.createConsumer()).thenReturn(consumer);
		when(properties.kafka()).thenReturn(new CatedraIntegrationProperties.KafkaProperties(
				"localhost:19092", "test-consumer-group", TOPIC));
		verifier = new CatedraKafkaConnectivityVerifier(consumerFactory, properties);
	}

	@Test
	void queriesConfiguredTopicAndClosesConsumerWithoutConsuming() {
		when(consumer.partitionsFor(TOPIC, TIMEOUT)).thenReturn(List.of(mock(PartitionInfo.class)));

		verifier.verifyConnectivity();

		verify(consumer).partitionsFor(TOPIC, TIMEOUT);
		verify(consumer).close();
		verifyNoMoreInteractions(consumer);
	}

	@Test
	void treatsEmptyMetadataAsInfrastructureFailure() {
		when(consumer.partitionsFor(TOPIC, TIMEOUT)).thenReturn(List.of());

		var exception = catchThrowableOfType(verifier::verifyConnectivity, CatedraKafkaConnectivityException.class);

		assertThat(exception.reason()).isEqualTo(Reason.INFRASTRUCTURE);
		verify(consumer).close();
	}

	@Test
	void classifiesNestedAuthenticationFailureAsAccessFailure() {
		assertFailure(new KafkaException("sensitive-placeholder",
				new AuthenticationException("sensitive-placeholder")), Reason.ACCESS);
	}

	@Test
	void classifiesAuthorizationFailureAsAccessFailure() {
		assertFailure(new AuthorizationException("sensitive-placeholder"), Reason.ACCESS);
	}

	@Test
	void classifiesNestedTimeoutAsUnavailable() {
		assertFailure(new KafkaException("sensitive-placeholder",
				new TimeoutException("sensitive-placeholder")), Reason.UNAVAILABLE);
	}

	@Test
	void classifiesOtherKafkaFailuresAsInfrastructureFailures() {
		assertFailure(new KafkaException("sensitive-placeholder"), Reason.INFRASTRUCTURE);
	}

	private void assertFailure(KafkaException failure, Reason expectedReason) {
		when(consumer.partitionsFor(TOPIC, TIMEOUT)).thenThrow(failure);

		var exception = catchThrowableOfType(verifier::verifyConnectivity, CatedraKafkaConnectivityException.class);

		assertThat(exception.reason()).isEqualTo(expectedReason);
		assertThat(exception.getMessage()).doesNotContain("sensitive-placeholder");
		assertThat(exception.getCause()).isNull();
		verify(consumer).close();
	}
}
