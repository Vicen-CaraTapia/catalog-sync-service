package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.verification;

import java.time.Duration;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.CatedraKafkaConnectivityException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

@Component
public class CatedraKafkaConnectivityVerifier {

	private static final Duration METADATA_TIMEOUT = Duration.ofSeconds(5);

	private final ConsumerFactory<String, String> consumerFactory;
	private final CatedraIntegrationProperties properties;

	public CatedraKafkaConnectivityVerifier(
			ConsumerFactory<String, String> consumerFactory,
			CatedraIntegrationProperties properties) {
		this.consumerFactory = consumerFactory;
		this.properties = properties;
	}

	public void verifyConnectivity() {
		try (var consumer = consumerFactory.createConsumer()) {
			var partitions = consumer.partitionsFor(
					properties.kafka().catalogTopic(),
					METADATA_TIMEOUT
			);

			if (partitions.isEmpty()) {
				throw new CatedraKafkaConnectivityException(
						CatedraKafkaConnectivityException.Reason.INFRASTRUCTURE
				);
			}
		} catch (KafkaException exception) {
			throw new CatedraKafkaConnectivityException(classify(exception));
		}
	}

	private static CatedraKafkaConnectivityException.Reason classify(KafkaException exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof AuthenticationException || cause instanceof AuthorizationException) {
				return CatedraKafkaConnectivityException.Reason.ACCESS;
			}

			if (cause instanceof TimeoutException) {
				return CatedraKafkaConnectivityException.Reason.UNAVAILABLE;
			}

			if (cause.getCause() == cause) {
				break;
			}
			cause = cause.getCause();
		}

		return CatedraKafkaConnectivityException.Reason.INFRASTRUCTURE;
	}
}
