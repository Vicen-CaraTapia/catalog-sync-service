package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry;

import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.exception.SynchronizationRetryInterruptedException;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** One bounded retry budget per invocation, with no transactions or Kafka offset operations. */
@Component
public final class SynchronizationRetryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(SynchronizationRetryExecutor.class);

    public enum Operation {
        INITIALIZATION, RECONCILIATION, CATALOG_NOTIFICATION
    }

    @FunctionalInterface
    public interface BackoffWaiter {
        void await(Duration duration) throws InterruptedException;
    }

    private final CatalogSynchronizationRetryProperties properties;
    private final TransientSynchronizationFailureClassifier classifier;
    private final BackoffWaiter waiter;

    @Autowired
    public SynchronizationRetryExecutor(
            CatalogSynchronizationRetryProperties properties,
            TransientSynchronizationFailureClassifier classifier
    ) {
        this(properties, classifier, Thread::sleep);
    }

    public SynchronizationRetryExecutor(
            CatalogSynchronizationRetryProperties properties,
            TransientSynchronizationFailureClassifier classifier,
            BackoffWaiter waiter
    ) {
        this.properties = Objects.requireNonNull(properties);
        this.classifier = Objects.requireNonNull(classifier);
        this.waiter = Objects.requireNonNull(waiter);
    }

    public void execute(Operation operation, Runnable action) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(action);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            checkInterrupted(operation);
            try {
                action.run();
                if (attempt > 1) {
                    LOG.info("Synchronization recovered: operation={}, attempt={}", operation, attempt);
                }
                return;
            } catch (RuntimeException failure) {
                if (Thread.currentThread().isInterrupted() || classifier.isInterruptedFailure(failure)) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Synchronization interrupted: operation={}", operation);
                    throw failure;
                }
                if (!classifier.isRetryable(failure)) {
                    LOG.error("Synchronization failed without retry: operation={}, attempt={}", operation, attempt);
                    throw failure;
                }
                lastFailure = failure;
                if (attempt < properties.maxAttempts()) {
                    Duration delay = attempt == 1 ? properties.firstBackoff() : properties.secondBackoff();
                    LOG.warn("Synchronization retry scheduled: operation={}, attempt={}, delayMillis={}",
                            operation, attempt, delay.toMillis());
                    await(operation, delay);
                }
            }
        }
        LOG.error("Synchronization retry attempts exhausted: operation={}, attempts={}",
                operation, properties.maxAttempts());
        throw Objects.requireNonNull(lastFailure);
    }

    private void await(Operation operation, Duration delay) {
        try {
            waiter.await(delay);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            LOG.warn("Synchronization backoff interrupted: operation={}", operation);
            throw new SynchronizationRetryInterruptedException(interruption);
        }
    }

    private void checkInterrupted(Operation operation) {
        if (Thread.currentThread().isInterrupted()) {
            LOG.warn("Synchronization interrupted before attempt: operation={}", operation);
            throw new SynchronizationRetryInterruptedException(new InterruptedException("Retry execution interrupted"));
        }
    }
}
