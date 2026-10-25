package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

/** Real Redis and PostgreSQL, with a synthetic notification and no Kafka offset commit. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.flyway.enabled=false",
                "catalog.initialization.enabled=false",
                "catalog.incremental-consumer.enabled=false"
        }
)
@ActiveProfiles("test")
class CatedraCatalogLiveNoOpIT {

    @Autowired
    private ReadAppliedCatalogVersionPort appliedVersionPort;

    @Autowired
    private ReadCatalogIncrementalChangesPort changesPort;

    @Autowired
    private CatalogUpdatedListener listener;

    @Autowired
    private DataSource dataSource;

    @Test
    void repeatedNotificationReadsRealMetadataWithoutChangingTheLocalCatalog() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long localVersion = appliedVersionPort.readAppliedVersion();
        long remoteVersion = changesPort.readMetadata().currentVersion();

        // This live check must never enter the incremental write path.
        assertThat(localVersion)
                .as("Local and Redis versions must match before this read-only live check")
                .isNotNull()
                .isEqualTo(remoteVersion);

        long[] counts = counts(jdbc);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        String eventJson = """
                {"eventId":"catalog-live-noop-check","eventType":"CatalogUpdated",
                 "newVersion":%d,"occurredAt":"2026-09-10T13:44:59Z",
                 "source":"LIVE_CHECK","schemaVersion":1}
                """.formatted(remoteVersion);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "catalog-live-check", 0, 0L, Long.toString(remoteVersion), eventJson);

        listener.onMessage(record, acknowledgment);

        assertThat(appliedVersionPort.readAppliedVersion()).isEqualTo(localVersion);
        assertThat(counts(jdbc)).containsExactly(counts);
        verify(acknowledgment).acknowledge();
    }

    private static long[] counts(JdbcTemplate jdbc) {
        return new long[] {
                jdbc.queryForObject("SELECT count(*) FROM professional_categories", Long.class),
                jdbc.queryForObject("SELECT count(*) FROM professionals", Long.class),
                jdbc.queryForObject("SELECT count(*) FROM weekly_schedules", Long.class)
        };
    }
}
