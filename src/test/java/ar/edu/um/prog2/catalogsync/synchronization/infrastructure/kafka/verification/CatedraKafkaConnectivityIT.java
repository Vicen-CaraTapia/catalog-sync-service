package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.verification;

import static org.assertj.core.api.Assertions.assertThatCode;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.verification.CatedraKafkaConnectivityVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
						+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
						+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
						+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
		}
)
@ActiveProfiles("kafka-integration")
class CatedraKafkaConnectivityIT {

	@Test
	void catedraKafkaTopicMetadataIsAvailable(@Autowired CatedraKafkaConnectivityVerifier verifier) {
		assertThatCode(verifier::verifyConnectivity)
				.doesNotThrowAnyException();
	}
}
