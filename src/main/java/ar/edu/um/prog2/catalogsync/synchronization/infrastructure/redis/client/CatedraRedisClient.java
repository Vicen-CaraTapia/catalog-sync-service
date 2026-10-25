package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisAccessException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import io.lettuce.core.RedisCommandExecutionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public final class CatedraRedisClient {

	private static final String CURRENT_VERSION_KEY = "catedra:sync:current-version";
	private static final String METADATA_KEY = "catedra:sync:metadata";
	private static final String CHANGES_KEY_PREFIX = "catedra:sync:changes:";
	private static final String CATEGORIES_KEY = "catedra:sync:professional-categories";
	private static final String PROFESSIONALS_KEY = "catedra:sync:professionals";
	private static final String SCHEDULES_KEY = "catedra:sync:weekly-schedules";
	private static final List<String> METADATA_FIELDS = List.of(
			"currentVersion", "oldestAvailableVersion", "publishedAt"
	);

	private final StringRedisTemplate redisTemplate;

	public CatedraRedisClient(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void verifyReadAccess() {
		try {
			redisTemplate.opsForValue().get(CURRENT_VERSION_KEY);
		} catch (DataAccessException exception) {
			throw translate(exception);
		}
	}

	public Map<String, String> readMetadataFields() {
		try {
			List<String> values = redisTemplate.<String, String>opsForHash()
					.multiGet(METADATA_KEY, METADATA_FIELDS);
			if (values == null || values.size() != METADATA_FIELDS.size()) {
				throw new InvalidCatedraRedisDataException();
			}
			Map<String, String> fields = new HashMap<>();
			for (int index = 0; index < METADATA_FIELDS.size(); index++) {
				fields.put(METADATA_FIELDS.get(index), values.get(index));
			}
			return fields;
		} catch (DataAccessException exception) {
			throw translate(exception);
		}
	}

	public Optional<String> readChangesJson(long version) {
		try {
			return Optional.ofNullable(
					redisTemplate.opsForValue().get(CHANGES_KEY_PREFIX + version)
			);
		} catch (DataAccessException exception) {
			throw translate(exception);
		}
	}

	public List<String> readProfessionalCategoryJson(List<Long> ids) {
		return readEntityJson(CATEGORIES_KEY, ids);
	}

	public List<String> readProfessionalJson(List<Long> ids) {
		return readEntityJson(PROFESSIONALS_KEY, ids);
	}

	public List<String> readWeeklyScheduleJson(List<Long> ids) {
		return readEntityJson(SCHEDULES_KEY, ids);
	}

	private List<String> readEntityJson(String hashKey, List<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		try {
			List<String> fields = ids.stream().map(String::valueOf).toList();
			List<String> values = redisTemplate.<String, String>opsForHash()
					.multiGet(hashKey, fields);
			if (values == null || values.size() != ids.size()
					|| values.stream().anyMatch(Objects::isNull)) {
				throw new InvalidCatedraRedisDataException();
			}
			return List.copyOf(values);
		} catch (DataAccessException exception) {
			throw translate(exception);
		}
	}

	private RuntimeException translate(DataAccessException exception) {
		if (hasCause(exception, RedisCommandExecutionException.class)) {
			return new CatedraRedisAccessException(exception);
		}
		return new CatedraRedisUnavailableException(exception);
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
