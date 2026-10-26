package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.verification;

import ar.edu.um.prog2.catalogsynctest.redis.RedisIntegrationTestApplication;

import static org.assertj.core.api.Assertions.assertThatCode;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client.CatedraRedisClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
		classes = RedisIntegrationTestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("redis-integration")
class CatedraRedisConnectivityIT {

	@Test
	void catedraRedisReadAccessIsAvailable(@Autowired CatedraRedisClient catedraRedisClient) {
		assertThatCode(catedraRedisClient::verifyReadAccess)
				.doesNotThrowAnyException();
	}
}
