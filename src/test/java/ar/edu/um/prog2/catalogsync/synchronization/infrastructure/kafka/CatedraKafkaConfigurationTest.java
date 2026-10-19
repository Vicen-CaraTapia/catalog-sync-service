package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

class CatedraKafkaConfigurationTest {

	@Test
	void consumerFactoryUsesCatedraConfiguration() {
		var properties = mock(CatedraIntegrationProperties.class);

		when(properties.kafka()).thenReturn(
				new CatedraIntegrationProperties.KafkaProperties(
						"127.0.0.1:19092",
						"test-consumer-group",
						"test-catalog-topic"
				)
		);

		var factory = new CatedraKafkaConfiguration()
				.catedraKafkaConsumerFactory(properties);

		assertThat(factory.getConfigurationProperties())
				.containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:19092")
				.containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group")
				.containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
				.containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
				.containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
				.containsEntry(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
	}
}
