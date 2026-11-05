package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.verification;

import static org.assertj.core.api.Assertions.assertThatCode;

import ar.edu.um.prog2.catalogsynctest.kafka.KafkaIntegrationTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
		classes = KafkaIntegrationTestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("kafka-integration")
class CatedraKafkaConnectivityIT {

	@Test
	void catedraKafkaTopicMetadataIsAvailable(@Autowired CatedraKafkaConnectivityVerifier verifier) {
		assertThatCode(verifier::verifyConnectivity)
				.doesNotThrowAnyException();
	}
}
