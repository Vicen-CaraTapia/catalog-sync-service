package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThatCode;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client.CatedraRedisClient;
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
@ActiveProfiles("redis-integration")
class CatedraRedisConnectivityIT {

	@Test
	void catedraRedisReadAccessIsAvailable(@Autowired CatedraRedisClient catedraRedisClient) {
		assertThatCode(catedraRedisClient::verifyReadAccess)
				.doesNotThrowAnyException();
	}
}
