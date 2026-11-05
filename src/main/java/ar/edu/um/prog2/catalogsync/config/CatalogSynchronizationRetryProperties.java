package ar.edu.um.prog2.catalogsync.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Constructor validation also protects callers that create the immutable settings directly. */
@ConfigurationProperties(prefix = "catalog.synchronization.retry")
public record CatalogSynchronizationRetryProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1s") Duration firstBackoff,
        @DefaultValue("2s") Duration secondBackoff
) {

    public CatalogSynchronizationRetryProperties {
        if (maxAttempts < 1 || maxAttempts > 3) {
            throw new IllegalArgumentException("Synchronization retry attempts must be between 1 and 3");
        }
        if (!validDelay(firstBackoff, Duration.ofSeconds(1))
                || !validDelay(secondBackoff, Duration.ofSeconds(2))
                || firstBackoff.compareTo(secondBackoff) > 0) {
            throw new IllegalArgumentException(
                    "Synchronization backoffs must be positive, nondecreasing and at most 1s and 2s respectively");
        }
    }

    private static boolean validDelay(Duration delay, Duration maximum) {
        return delay != null && !delay.isNegative() && !delay.isZero() && delay.compareTo(maximum) <= 0;
    }
}
