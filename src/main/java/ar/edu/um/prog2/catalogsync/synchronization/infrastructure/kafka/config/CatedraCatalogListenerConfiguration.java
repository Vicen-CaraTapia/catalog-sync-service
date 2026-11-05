package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

@Configuration(proxyBeanMethods = false)
@EnableKafka
class CatedraCatalogListenerConfiguration {

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, String> catedraCatalogListenerContainerFactory(
			@Qualifier("catedraKafkaConsumerFactory") ConsumerFactory<String, String> consumerFactory
	) {
		var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
		factory.setConsumerFactory(consumerFactory);
		factory.setAutoStartup(false);
		factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
		factory.setCommonErrorHandler(new CommonContainerStoppingErrorHandler());
		return factory;
	}
}
