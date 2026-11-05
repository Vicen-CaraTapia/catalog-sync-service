package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisAccessException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import io.lettuce.core.RedisCommandExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CatedraRedisIncrementalReadTest {

    private static final List<String> METADATA_FIELDS = List.of(
            "currentVersion", "oldestAvailableVersion", "publishedAt"
    );

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, String, String> hashOperations;

    private CatedraRedisClient client;

    @BeforeEach
    void setUp() {
        client = new CatedraRedisClient(redisTemplate);
    }

    @Test
    void readsOnlyContractualMetadataFieldsInOneHashOperation() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet("catedra:sync:metadata", METADATA_FIELDS))
                .thenReturn(List.of("7", "4", "2026-09-10T13:44:59Z"));

        var fields = client.readMetadataFields();

        assertThat(fields).containsEntry("currentVersion", "7")
                .containsEntry("oldestAvailableVersion", "4")
                .containsEntry("publishedAt", "2026-09-10T13:44:59Z");
        verify(hashOperations).multiGet("catedra:sync:metadata", METADATA_FIELDS);
    }

    @Test
    void readsVersionSpecificChangesAndReportsOnlyAbsentKeyAsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catedra:sync:changes:7")).thenReturn(null, "{\"version\":7}");

        assertThat(client.readChangesJson(7)).isEmpty();
        assertThat(client.readChangesJson(7)).contains("{\"version\":7}");
        verify(valueOperations, org.mockito.Mockito.times(2)).get("catedra:sync:changes:7");
    }

    @Test
    void readsAffectedEntityFieldsFromOnlyTheContractualHashes() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet("catedra:sync:professional-categories", List.of("1")))
                .thenReturn(List.of("category-json"));
        when(hashOperations.multiGet("catedra:sync:professionals", List.of("15", "15")))
                .thenReturn(List.of("professional-json", "professional-json"));
        when(hashOperations.multiGet("catedra:sync:weekly-schedules", List.of("21")))
                .thenReturn(List.of("schedule-json"));

        assertThat(client.readProfessionalCategoryJson(List.of(1L))).containsExactly("category-json");
        assertThat(client.readProfessionalJson(List.of(15L, 15L)))
                .containsExactly("professional-json", "professional-json");
        assertThat(client.readWeeklyScheduleJson(List.of(21L))).containsExactly("schedule-json");
        verify(hashOperations).multiGet("catedra:sync:professional-categories", List.of("1"));
        verify(hashOperations).multiGet("catedra:sync:professionals", List.of("15", "15"));
        verify(hashOperations).multiGet("catedra:sync:weekly-schedules", List.of("21"));
    }

    @Test
    void skipsHashAccessForEmptyAffectedIds() {
        assertThat(client.readProfessionalCategoryJson(List.of())).isEmpty();
        assertThat(client.readProfessionalJson(List.of())).isEmpty();
        assertThat(client.readWeeklyScheduleJson(List.of())).isEmpty();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void missingAffectedEntityFieldFailsInsteadOfInventingDeletion() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet("catedra:sync:professionals", List.of("15", "16")))
                .thenReturn(Arrays.asList("professional-json", null));

        assertThatThrownBy(() -> client.readProfessionalJson(List.of(15L, 16L)))
                .isInstanceOf(InvalidCatedraRedisDataException.class);
    }

    @Test
    void aclFailureAndUnavailableRedisRemainExplicitErrors() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catedra:sync:changes:7"))
                .thenThrow(new RedisSystemException("Redis rejected command", new RedisCommandExecutionException("NOPERM")))
                .thenThrow(new RedisConnectionFailureException("Connection failed"));

        assertThatThrownBy(() -> client.readChangesJson(7))
                .isInstanceOf(CatedraRedisAccessException.class);
        assertThatThrownBy(() -> client.readChangesJson(7))
                .isInstanceOf(CatedraRedisUnavailableException.class);
    }
}
