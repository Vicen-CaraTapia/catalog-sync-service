package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisAccessException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import io.lettuce.core.RedisCommandExecutionException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public final class CatedraRedisClient {

	private static final String CURRENT_VERSION_KEY = "catedra:sync:current-version";

	private final StringRedisTemplate redisTemplate;

	public CatedraRedisClient(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void verifyReadAccess() {
		try {
			redisTemplate.opsForValue().get(CURRENT_VERSION_KEY);
		} catch (DataAccessException exception) {
			if (hasCause(exception, RedisCommandExecutionException.class)) {
				throw new CatedraRedisAccessException(exception);
			}

			throw new CatedraRedisUnavailableException(exception);
		}
	}

	private boolean hasCause(
			Throwable failure,
			Class<? extends Throwable> expectedCauseType
	) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (expectedCauseType.isInstance(cause)) {
				return true;
			}
		}

		return false;
	}
}
