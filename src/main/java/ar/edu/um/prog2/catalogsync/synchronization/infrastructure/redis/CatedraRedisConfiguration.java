package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration(proxyBeanMethods = false)
public class CatedraRedisConfiguration {

	@Bean
	LettuceConnectionFactory catedraRedisConnectionFactory(
			CatedraIntegrationProperties integrationProperties
	) {
		var redis = integrationProperties.redis();

		var standaloneConfiguration =
				new RedisStandaloneConfiguration(redis.host(), redis.port());

		standaloneConfiguration.setUsername(redis.username());
		standaloneConfiguration.setPassword(RedisPassword.of(redis.password()));

		var clientOptions = ClientOptions.builder()
				.socketOptions(SocketOptions.builder()
						.connectTimeout(redis.connectTimeout())
						.build())
				.build();

		var clientConfiguration = LettuceClientConfiguration.builder()
				.commandTimeout(redis.commandTimeout())
				.clientOptions(clientOptions)
				.build();

		return new LettuceConnectionFactory(
				standaloneConfiguration,
				clientConfiguration
		);
	}
}
