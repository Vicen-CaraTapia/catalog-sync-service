package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        classes = RedisIntegrationTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
class CatedraRedisIncrementalIT {

    private static final String TEST_PASSWORD = "redis-local-fixture-password";
    private static final LocalRedisServer REDIS = LocalRedisServer.start();
    private static final List<String> FIXTURE_KEYS = List.of(
            "catedra:sync:metadata",
            "catedra:sync:changes:7",
            "catedra:sync:professional-categories",
            "catedra:sync:professionals",
            "catedra:sync:weekly-schedules"
    );

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ReadCatalogIncrementalChangesPort catalogChanges;

    @DynamicPropertySource
    static void localRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("catedra.redis.host", () -> "127.0.0.1");
        registry.add("catedra.redis.port", REDIS::port);
        registry.add("catedra.redis.username", () -> "default");
        registry.add("catedra.redis.password", () -> TEST_PASSWORD);
    }

    @AfterAll
    static void stopLocalRedis() {
        REDIS.close();
    }

    @BeforeEach
    void clearFixtures() {
        redisTemplate.delete(FIXTURE_KEYS);
    }

    @Test
    void readsMetadataChangesAndAffectedCurrentEntitiesThroughRealRedis() {
        redisTemplate.opsForHash().put("catedra:sync:metadata", "currentVersion", "7");
        redisTemplate.opsForHash().put("catedra:sync:metadata", "oldestAvailableVersion", "4");
        redisTemplate.opsForHash().put("catedra:sync:metadata", "publishedAt", "2026-09-10T13:44:59Z");
        redisTemplate.opsForValue().set("catedra:sync:changes:7", """
                {"version":7,"changes":{"professionalCategories":[1],"professionals":[15],"weeklySchedules":[21]}}
                """);
        redisTemplate.opsForHash().put("catedra:sync:professional-categories", "1", """
                {"id":1,"name":"Clinica medica","description":null,"enabled":false,
                 "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                """);
        redisTemplate.opsForHash().put("catedra:sync:professionals", "15", """
                {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true,
                 "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                """);
        redisTemplate.opsForHash().put("catedra:sync:weekly-schedules", "21", """
                {"id":21,"professionalId":15,"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00:00",
                 "slotDurationMinutes":30,"enabled":false,
                 "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                """);

        var metadata = catalogChanges.readMetadata();
        var changes = catalogChanges.readChanges(7).orElseThrow();

        assertThat(metadata.currentVersion()).isEqualTo(7);
        assertThat(metadata.oldestAvailableVersion()).isEqualTo(4);
        assertThat(metadata.publishedAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
        assertThat(changes.requestedChangeVersion()).isEqualTo(7);
        assertThat(changes.currentProfessionalCategories()).singleElement().satisfies(category -> {
            assertThat(category.id()).isEqualTo(1);
            assertThat(category.description()).isNull();
            assertThat(category.enabled()).isFalse();
        });
        assertThat(changes.currentProfessionals()).singleElement().satisfies(professional -> {
            assertThat(professional.id()).isEqualTo(15);
            assertThat(professional.categoryId()).isEqualTo(1);
            assertThat(professional.enabled()).isTrue();
        });
        assertThat(changes.currentWeeklySchedules()).singleElement().satisfies(schedule -> {
            assertThat(schedule.id()).isEqualTo(21);
            assertThat(schedule.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(schedule.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(schedule.enabled()).isFalse();
        });
    }

    @Test
    void reportsOnlyAnAbsentChangesetKeyAsEmpty() {
        assertThat(catalogChanges.readChanges(7)).isEmpty();
    }

    @Test
    void rejectsMalformedChangesetStoredInRedis() {
        redisTemplate.opsForValue().set("catedra:sync:changes:7", "{not-json");

        assertThatThrownBy(() -> catalogChanges.readChanges(7))
                .isInstanceOf(InvalidCatedraRedisDataException.class);
    }

    @Test
    void rejectsMissingAffectedEntityHashField() {
        redisTemplate.opsForValue().set("catedra:sync:changes:7", """
                {"version":7,"changes":{"professionalCategories":[],"professionals":[15],"weeklySchedules":[]}}
                """);

        assertThatThrownBy(() -> catalogChanges.readChanges(7))
                .isInstanceOf(InvalidCatedraRedisDataException.class);
    }

    @Test
    void rejectsIncompleteMetadataHash() {
        redisTemplate.opsForHash().put("catedra:sync:metadata", "currentVersion", "7");
        redisTemplate.opsForHash().put("catedra:sync:metadata", "oldestAvailableVersion", "4");

        assertThatThrownBy(catalogChanges::readMetadata)
                .isInstanceOf(InvalidCatedraRedisDataException.class);
    }

    private record LocalRedisServer(Process process, int port) implements AutoCloseable {

        static LocalRedisServer start() {
            try {
                int port;
                try (var socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
                    port = socket.getLocalPort();
                }

                var process = new ProcessBuilder(
                        "redis-server", "--bind", "127.0.0.1", "--port", Integer.toString(port),
                        "--save", "", "--appendonly", "no", "--requirepass", TEST_PASSWORD
                ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                var server = new LocalRedisServer(process, port);
                server.waitUntilReady();
                return server;
            } catch (IOException exception) {
                throw new IllegalStateException("A local redis-server executable is required for this test.", exception);
            }
        }

        private void waitUntilReady() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline && process.isAlive()) {
                try (var socket = new Socket()) {
                    socket.connect(new InetSocketAddress("127.0.0.1", port), 100);
                    return;
                } catch (IOException exception) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            process.destroyForcibly();
            throw new IllegalStateException("Local Redis did not start within five seconds.");
        }

        @Override
        public void close() {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
