package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.TransientSynchronizationFailureClassifier;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CatalogReconciliationRunnerTest {

    @Test
    void invokesReconciliationOnce() {
        var calls = new AtomicInteger();

        new CatalogReconciliationRunner(calls::incrementAndGet, retryExecutor(new ArrayList<>()))
                .run(new DefaultApplicationArguments());

        assertThat(calls).hasValue(1);
    }

    @Test
    void propagatesOnlyFixedFailureWithoutLoggingSensitiveCause(CapturedOutput output) {
        var runner = new CatalogReconciliationRunner(() -> {
            throw new IllegalStateException("secret-marker-must-not-be-logged");
        }, retryExecutor(new ArrayList<>()));

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Catalog state reconciliation failed; application startup aborted.")
                .hasNoCause();
        assertThat(output.getAll())
                .contains("Catalog state reconciliation failed; application startup aborted.")
                .doesNotContain("secret-marker-must-not-be-logged");
    }

    @Test
    void retriesTheCompleteReconciliationBeforeReturning() {
        var calls = new AtomicInteger();
        var delays = new ArrayList<Duration>();
        var runner = new CatalogReconciliationRunner(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new CatedraRedisUnavailableException(new ConnectException());
            }
        }, retryExecutor(delays));

        runner.run(new DefaultApplicationArguments());

        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(Duration.ofSeconds(1));
    }

    private static SynchronizationRetryExecutor retryExecutor(ArrayList<Duration> delays) {
        return new SynchronizationRetryExecutor(
                new CatalogSynchronizationRetryProperties(3, Duration.ofSeconds(1), Duration.ofSeconds(2)),
                new TransientSynchronizationFailureClassifier(), delays::add);
    }
}
