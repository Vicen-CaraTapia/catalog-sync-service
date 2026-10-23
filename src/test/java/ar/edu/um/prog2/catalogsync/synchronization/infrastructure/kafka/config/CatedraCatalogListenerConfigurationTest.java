package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

class CatedraCatalogListenerConfigurationTest {

	@Test
	void reusesCatedraConsumerFactoryAndKeepsListenerInactiveWithManualAcknowledgment() {
		var properties = mock(CatedraIntegrationProperties.class);
		when(properties.kafka()).thenReturn(new CatedraIntegrationProperties.KafkaProperties(
				"127.0.0.1:19092", "assigned-consumer-group", "assigned-catalog-topic"
		));

		new ApplicationContextRunner()
				.withBean(CatedraIntegrationProperties.class, () -> properties)
				.withUserConfiguration(CatedraKafkaConfiguration.class, CatedraCatalogListenerConfiguration.class)
				.run(context -> {
					assertThat(context).hasNotFailed();

					@SuppressWarnings("unchecked")
					var consumerFactory = (ConsumerFactory<String, String>) context.getBean(
							"catedraKafkaConsumerFactory", ConsumerFactory.class
					);
					@SuppressWarnings("unchecked")
					var listenerFactory = (ConcurrentKafkaListenerContainerFactory<String, String>) context.getBean(
							"catedraCatalogListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class
					);

					assertThat(listenerFactory.getConsumerFactory()).isSameAs(consumerFactory);
					assertThat(consumerFactory.getConfigurationProperties())
							.containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "assigned-consumer-group")
							.containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

					var container = listenerFactory.createContainer("assigned-catalog-topic");
					assertThat(container.isAutoStartup()).isFalse();
					assertThat(container.isRunning()).isFalse();
					assertThat(container.getContainerProperties().getAckMode()).isEqualTo(AckMode.MANUAL_IMMEDIATE);
					assertThat(container.getCommonErrorHandler())
							.isInstanceOf(CommonContainerStoppingErrorHandler.class);
				});
	}
}
