package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisAccessException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CatedraRedisClientTest {

	private static final String CURRENT_VERSION_KEY = "catedra:sync:current-version";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private CatedraRedisClient client;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		client = new CatedraRedisClient(redisTemplate);
	}

	@Test
	void verifyReadAccessReadsOnlyTheContractualCurrentVersionKey() {
		client.verifyReadAccess();

		verify(valueOperations).get(CURRENT_VERSION_KEY);
	}

	@Test
	void verifyReadAccessAllowsAnAbsentValue() {
		when(valueOperations.get(CURRENT_VERSION_KEY)).thenReturn(null);

		assertThatCode(client::verifyReadAccess)
				.doesNotThrowAnyException();
	}

	@Test
	void verifyReadAccessTranslatesAclFailuresToAccessException() {
		var failure = new RedisSystemException(
				"Redis command failed",
				new RedisCommandExecutionException("NOPERM")
		);
		when(valueOperations.get(CURRENT_VERSION_KEY)).thenThrow(failure);

		assertThatThrownBy(client::verifyReadAccess)
				.isInstanceOf(CatedraRedisAccessException.class)
				.hasCause(failure);
	}

	@Test
	void verifyReadAccessTranslatesUnavailableRedisToUnavailableException() {
		var failure = new RedisConnectionFailureException("Redis connection failed");
		when(valueOperations.get(CURRENT_VERSION_KEY)).thenThrow(failure);

		assertThatThrownBy(client::verifyReadAccess)
				.isInstanceOf(CatedraRedisUnavailableException.class)
				.hasCause(failure);
	}

	@Test
	void verifyReadAccessTranslatesTimeoutToUnavailableException() {
		var failure = new RedisConnectionFailureException(
				"Redis command timed out",
				new RedisCommandTimeoutException("Command timed out")
		);
		when(valueOperations.get(CURRENT_VERSION_KEY)).thenThrow(failure);

		assertThatThrownBy(client::verifyReadAccess)
				.isInstanceOf(CatedraRedisUnavailableException.class)
				.hasCause(failure);
	}
}
