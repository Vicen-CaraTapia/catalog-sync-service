package ar.edu.um.prog2.catalogsynctest.config;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import java.time.Duration;

/** Shared fictitious integration settings for tests using a controlled REST server. */
public final class CatedraTestProperties {
    private CatedraTestProperties() { }

    public static CatedraIntegrationProperties forRest(CatedraIntegrationProperties.RestProperties rest) {
        return new CatedraIntegrationProperties(
                "test-catalog-sync",
                "test-technical-jwt",
                rest,
                new CatedraIntegrationProperties.RedisProperties(
                        "localhost", 6379, "test-user", "test-password",
                        "catedra:sync:*", "alumnos:test-catalog-sync:*",
                        Duration.ofSeconds(2), Duration.ofSeconds(5)
                ),
                new CatedraIntegrationProperties.KafkaProperties(
                        "localhost:19092", "test-catalog-sync-consumer", "catedra.catalog.test-catalog-sync"
                )
        );
    }
}
