package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.config;

import java.util.HashMap;
import java.util.Map;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration(proxyBeanMethods = false)
public class CatedraKafkaConfiguration {

	@Bean
	ConsumerFactory<String, String> catedraKafkaConsumerFactory(CatedraIntegrationProperties properties) {
		var kafka = properties.kafka();
		Map<String, Object> config = new HashMap<>();

		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers());
		config.put(ConsumerConfig.GROUP_ID_CONFIG, kafka.consumerGroupId());
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);

		return new DefaultKafkaConsumerFactory<>(config);
	}
}
