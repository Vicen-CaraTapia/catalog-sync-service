package ar.edu.um.prog2.catalogsync.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class CatedraIntegrationPropertiesValidationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(CatedraIntegrationPropertiesConfiguration.class);

	@Test
	void contextFailsWhenTechnicalJwtIsMissing() {
		contextRunner
				.withPropertyValues(
						"catedra.group-id=test-catalog-sync",
						"catedra.rest.base-url=http://localhost:18080",
						"catedra.redis.host=localhost",
						"catedra.redis.port=6379",
						"catedra.redis.username=test-user",
						"catedra.redis.password=test-only-password",
						"catedra.redis.read-namespace=catedra:sync:*",
						"catedra.redis.write-namespace=alumnos:test-catalog-sync:*",
						"catedra.kafka.bootstrap-servers=localhost:19092",
						"catedra.kafka.consumer-group-id=test-catalog-sync-consumer",
						"catedra.kafka.catalog-topic=catedra.catalog.test-catalog-sync"
				)
				.run(context -> {
					assertThat(context).hasFailed();

					Throwable failure = context.getStartupFailure();

					assertThat(failure)
							.isInstanceOf(ConfigurationPropertiesBindException.class)
							.hasRootCauseInstanceOf(BindValidationException.class)
							.hasStackTraceContaining("technicalJwt");
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(CatedraIntegrationProperties.class)
	static class CatedraIntegrationPropertiesConfiguration {
	}
}
