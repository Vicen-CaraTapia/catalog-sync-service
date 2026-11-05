package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor.Operation.RECONCILIATION;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.exception.InvalidCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.exception.SynchronizationRetryInterruptedException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraHttpException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTimeoutException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraTransportException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(OutputCaptureExtension.class)
class SynchronizationRetryExecutorTest {

    private final List<Duration> delays = new ArrayList<>();
    private final TransientSynchronizationFailureClassifier classifier = new TransientSynchronizationFailureClassifier();

    private SynchronizationRetryExecutor executor(int attempts) {
        return new SynchronizationRetryExecutor(properties(attempts), classifier, delays::add);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void firstAttemptSuccessDoesNotWaitOrOpenATransaction(int attempts) {
        var calls = new AtomicInteger();

        executor(attempts).execute(RECONCILIATION, () -> {
            calls.incrementAndGet();
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        });

        assertThat(calls).hasValue(1);
        assertThat(delays).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void successOnLaterAttemptsUsesTheExactBackoffPrefix(int successfulAttempt) {
        var calls = new AtomicInteger();
        var failure = new CatedraTimeoutException(new SocketTimeoutException("private"));

        executor(successfulAttempt).execute(RECONCILIATION, () -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            if (calls.incrementAndGet() < successfulAttempt) {
                throw failure;
            }
        });

        assertThat(calls).hasValue(successfulAttempt);
        assertThat(delays).containsExactlyElementsOf(
                List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)).subList(0, successfulAttempt - 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void exhaustsValidatedBudgetWithoutWaitingAfterFinalFailureAndPreservesLastException(int attempts) {
        var calls = new AtomicInteger();
        var failures = IntStream.range(0, attempts).mapToObj(index ->
                new CatedraTimeoutException(new SocketTimeoutException("private-" + index))).toList();

        assertThatThrownBy(() -> executor(attempts).execute(RECONCILIATION,
                () -> { throw failures.get(calls.getAndIncrement()); }))
                .isSameAs(failures.getLast());

        assertThat(calls).hasValue(attempts);
        assertThat(delays).containsExactlyElementsOf(
                List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)).subList(0, attempts - 1));
    }

    @Test
    void permanentContractFailureWrappingTimeoutFailsImmediately() {
        var calls = new AtomicInteger();
        var failure = new InvalidCatalogSnapshotException(new CatedraTimeoutException(new SocketTimeoutException()));

        assertThatThrownBy(() -> executor(3).execute(RECONCILIATION, () -> {
            calls.incrementAndGet();
            throw failure;
        })).isSameAs(failure);

        assertThat(calls).hasValue(1);
        assertThat(delays).isEmpty();
    }

    @Test
    void interruptedBackoffRestoresFlagAndPreventsTheNextAttempt() {
        var calls = new AtomicInteger();
        var interruption = new InterruptedException("private-interruption");
        var executor = new SynchronizationRetryExecutor(properties(3), classifier, duration -> {
            delays.add(duration);
            throw interruption;
        });
        try {
            assertThatThrownBy(() -> executor.execute(RECONCILIATION, () -> {
                calls.incrementAndGet();
                throw new CatedraTimeoutException(new SocketTimeoutException());
            })).isInstanceOf(SynchronizationRetryInterruptedException.class).hasCause(interruption);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(calls).hasValue(1);
            assertThat(delays).containsExactly(Duration.ofSeconds(1));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void alreadyInterruptedThreadDoesNotExecuteOrWait() {
        var calls = new AtomicInteger();
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(() -> executor(3).execute(RECONCILIATION, calls::incrementAndGet))
                    .isInstanceOf(SynchronizationRetryInterruptedException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(calls).hasValue(0);
            assertThat(delays).isEmpty();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void wrappedOperationInterruptionIsNotRetriedAndKeepsOriginalFailure() {
        var calls = new AtomicInteger();
        var failure = new CatedraTransportException(new InterruptedException("private-interruption"));
        try {
            assertThatThrownBy(() -> executor(3).execute(RECONCILIATION, () -> {
                calls.incrementAndGet();
                throw failure;
            })).isSameAs(failure);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(calls).hasValue(1);
            assertThat(delays).isEmpty();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptedOperationStopsImmediatelyEvenWhenTheFailureWouldOtherwiseBeTransient() {
        var calls = new AtomicInteger();
        var failure = new CatedraTimeoutException(new SocketTimeoutException());
        try {
            assertThatThrownBy(() -> executor(3).execute(RECONCILIATION, () -> {
                calls.incrementAndGet();
                Thread.currentThread().interrupt();
                throw failure;
            })).isSameAs(failure);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(calls).hasValue(1);
            assertThat(delays).isEmpty();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptionFlagSetByWaiterStillPreventsAnotherExecution() {
        var calls = new AtomicInteger();
        var executor = new SynchronizationRetryExecutor(properties(3), classifier,
                duration -> Thread.currentThread().interrupt());
        try {
            assertThatThrownBy(() -> executor.execute(RECONCILIATION, () -> {
                calls.incrementAndGet();
                throw new CatedraTimeoutException(new SocketTimeoutException());
            })).isInstanceOf(SynchronizationRetryInterruptedException.class);
            assertThat(calls).hasValue(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void eachInvocationGetsItsOwnBudget() {
        var executor = executor(3);
        var first = new AtomicInteger();
        assertThatThrownBy(() -> executor.execute(RECONCILIATION, () -> {
            first.incrementAndGet();
            throw new CatedraTimeoutException(new SocketTimeoutException());
        })).isInstanceOf(CatedraTimeoutException.class);
        var second = new AtomicInteger();

        executor.execute(RECONCILIATION, () -> {
            if (second.incrementAndGet() == 1) {
                throw new CatedraTimeoutException(new SocketTimeoutException());
            }
        });

        assertThat(first).hasValue(3);
        assertThat(second).hasValue(2);
        assertThat(delays).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1));
    }

    @Test
    void logsOnlyOperationAttemptAndDelayWithoutExceptionDetails(CapturedOutput output) {
        var calls = new AtomicInteger();
        var http = new CatedraHttpException(HttpStatusCode.valueOf(503), "secret-code-marker", "secret-jwt-body-marker");
        http.addSuppressed(new RuntimeException("secret-password-marker"));
        var network = new CatedraTransportException(new ConnectException("secret-network-marker"));

        executor(3).execute(RECONCILIATION, () -> {
            int attempt = calls.incrementAndGet();
            if (attempt == 1) {
                throw http;
            }
            if (attempt == 2) {
                throw network;
            }
        });

        assertThat(output.getAll()).contains("operation=RECONCILIATION", "attempt=1", "attempt=2",
                        "delayMillis=1000", "delayMillis=2000", "Synchronization recovered")
                .doesNotContain("secret-code-marker", "secret-jwt-body-marker", "secret-password-marker",
                        "secret-network-marker", "CatedraHttpException", "CatedraTransportException");
    }

    @Test
    void exhaustionAndPermanentFailureLogsNeverExposeCausesOrResponses(CapturedOutput output) {
        var transientFailure = new CatedraHttpException(HttpStatusCode.valueOf(503),
                "secret-exhausted-code", "secret-exhausted-response");
        transientFailure.addSuppressed(new RuntimeException("secret-suppressed-cause"));
        var permanentFailure = new InvalidCatalogSnapshotException(
                new RuntimeException("secret-invalid-payload"));

        assertThatThrownBy(() -> executor(3).execute(RECONCILIATION, () -> { throw transientFailure; }))
                .isSameAs(transientFailure);
        assertThatThrownBy(() -> executor(3).execute(RECONCILIATION, () -> { throw permanentFailure; }))
                .isSameAs(permanentFailure);

        assertThat(output.getAll()).contains("Synchronization retry attempts exhausted", "attempts=3",
                        "Synchronization failed without retry", "attempt=1")
                .doesNotContain("secret-exhausted-code", "secret-exhausted-response", "secret-suppressed-cause",
                        "secret-invalid-payload", "InvalidCatalogSnapshotException", "CatedraHttpException");
    }

    @Test
    void externalShorterWaitsAreUsedWithoutExpandingTheBudget() {
        var executor = new SynchronizationRetryExecutor(new CatalogSynchronizationRetryProperties(3,
                Duration.ofMillis(250), Duration.ofMillis(500)), classifier, delays::add);
        var calls = new AtomicInteger();

        executor.execute(RECONCILIATION, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new CatedraTimeoutException(new SocketTimeoutException());
            }
        });

        assertThat(calls).hasValue(3);
        assertThat(delays).containsExactly(Duration.ofMillis(250), Duration.ofMillis(500));
    }

    private static CatalogSynchronizationRetryProperties properties(int attempts) {
        return new CatalogSynchronizationRetryProperties(attempts, Duration.ofSeconds(1), Duration.ofSeconds(2));
    }
}
