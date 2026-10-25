package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import ar.edu.um.prog2.catalogsynctest.redis.RedisIntegrationTestApplication;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = RedisIntegrationTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("redis-integration")
class CatedraRedisCatalogChangesIT {

    @Autowired
    private ReadCatalogIncrementalChangesPort catalogChanges;

    @Test
    void readsCatedraSynchronizationMetadata() {
        var metadata = catalogChanges.readMetadata();

        assertThat(metadata.publishedAt()).isNotNull();
    }

    @Test
    void readsCurrentChangesetAndAffectedEntitiesWhenRetained() {
        var currentVersion = catalogChanges.readMetadata().currentVersion();
        var changes = catalogChanges.readChanges(currentVersion);

        Assumptions.assumeTrue(changes.isPresent(),
                "The catedra has no retained changeset for its current version.");
        assertThat(changes.orElseThrow().requestedChangeVersion()).isEqualTo(currentVersion);
    }
}
