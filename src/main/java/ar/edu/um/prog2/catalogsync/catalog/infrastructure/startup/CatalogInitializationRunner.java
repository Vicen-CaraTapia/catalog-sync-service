package ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor.Operation;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(0)
@ConditionalOnProperty(
        prefix = "catalog.initialization",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public final class CatalogInitializationRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogInitializationRunner.class);
    private static final String FAILURE_MESSAGE =
            "Initial catalog synchronization failed; application startup aborted.";

    private final InitializeCatalogUseCase initializeCatalogUseCase;
    private final SynchronizationRetryExecutor retryExecutor;

    public CatalogInitializationRunner(
            InitializeCatalogUseCase initializeCatalogUseCase,
            SynchronizationRetryExecutor retryExecutor
    ) {
        this.initializeCatalogUseCase = Objects.requireNonNull(initializeCatalogUseCase);
        this.retryExecutor = Objects.requireNonNull(retryExecutor);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            retryExecutor.execute(Operation.INITIALIZATION, initializeCatalogUseCase::initializeIfNeeded);
        }
        catch (RuntimeException exception) {
            LOG.error(FAILURE_MESSAGE);
            throw new IllegalStateException(FAILURE_MESSAGE);
        }
    }
}
