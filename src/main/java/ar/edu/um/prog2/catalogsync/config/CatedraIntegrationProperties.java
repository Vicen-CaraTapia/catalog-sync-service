package ar.edu.um.prog2.catalogsync.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "catedra")
@Validated
public record CatedraIntegrationProperties(
		@NotBlank String groupId,
		@NotBlank String technicalJwt,
		@NotNull @Valid RestProperties rest,
		@NotNull @Valid RedisProperties redis,
		@NotNull @Valid KafkaProperties kafka
) {

	@Override
	public String toString() {
		return "CatedraIntegrationProperties["
				+ "groupId=" + groupId
				+ ", technicalJwt=<redacted>"
				+ ", rest=" + rest
				+ ", redis=" + redis
				+ ", kafka=" + kafka
				+ "]";
	}

	public record RestProperties(
			@NotBlank String baseUrl,
			@NotNull @DurationMin(seconds = 0, inclusive = false) Duration connectTimeout,
			@NotNull @DurationMin(seconds = 0, inclusive = false) Duration readTimeout
	) {
	}

	public record RedisProperties(
			@NotBlank String host,
			@NotNull @Positive Integer port,
			@NotBlank String username,
			@NotBlank String password,
			@NotBlank String readNamespace,
			@NotBlank String writeNamespace
	) {

		@Override
		public String toString() {
			return "RedisProperties["
					+ "host=" + host
					+ ", port=" + port
					+ ", username=" + username
					+ ", password=<redacted>"
					+ ", readNamespace=" + readNamespace
					+ ", writeNamespace=" + writeNamespace
					+ "]";
		}
	}

	public record KafkaProperties(
			@NotBlank String bootstrapServers,
			@NotBlank String consumerGroupId,
			@NotBlank String catalogTopic
	) {
	}
}
