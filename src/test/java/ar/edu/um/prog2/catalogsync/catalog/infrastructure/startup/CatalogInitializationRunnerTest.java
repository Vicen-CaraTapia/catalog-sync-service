package ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.TransientSynchronizationFailureClassifier;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CatalogInitializationRunnerTest {

    @Test
    void invokesInitializationOnce() {
        AtomicInteger calls = new AtomicInteger();
        InitializeCatalogUseCase useCase = calls::incrementAndGet;

        new CatalogInitializationRunner(useCase, retryExecutor(new ArrayList<>()))
                .run(new DefaultApplicationArguments());

        assertThat(calls).hasValue(1);
    }

    @Test
    void logsOnlyFixedMessageAndFailsWithoutExposingCause(CapturedOutput output) {
        RuntimeException failure = new IllegalStateException("secret-marker-must-not-be-logged");
        CatalogInitializationRunner runner = new CatalogInitializationRunner(() -> {
            throw failure;
        }, retryExecutor(new ArrayList<>()));

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Initial catalog synchronization failed; application startup aborted.")
                .hasNoCause();

        assertThat(output.getAll())
                .contains("Initial catalog synchronization failed; application startup aborted.")
                .doesNotContain("secret-marker-must-not-be-logged");
    }

    @Test
    void retriesTheCompleteInitializationBeforeReturning() {
        AtomicInteger calls = new AtomicInteger();
        var delays = new ArrayList<Duration>();
        InitializeCatalogUseCase useCase = () -> {
            if (calls.incrementAndGet() == 1) {
                throw new CatedraRedisUnavailableException(new SocketTimeoutException());
            }
        };

        new CatalogInitializationRunner(useCase, retryExecutor(delays))
                .run(new DefaultApplicationArguments());

        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(Duration.ofSeconds(1));
    }

    private static SynchronizationRetryExecutor retryExecutor(ArrayList<Duration> delays) {
        return new SynchronizationRetryExecutor(
                new CatalogSynchronizationRetryProperties(3, Duration.ofSeconds(1), Duration.ofSeconds(2)),
                new TransientSynchronizationFailureClassifier(), delays::add);
    }
}
