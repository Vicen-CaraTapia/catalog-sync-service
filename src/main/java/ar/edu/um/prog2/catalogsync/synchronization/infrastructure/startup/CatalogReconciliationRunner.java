package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.startup;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ReconcileCatalogStateUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor.Operation;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(
        prefix = "catalog.reconciliation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public final class CatalogReconciliationRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogReconciliationRunner.class);
    private static final String FAILURE_MESSAGE =
            "Catalog state reconciliation failed; application startup aborted.";

    private final ReconcileCatalogStateUseCase reconcileCatalog;
    private final SynchronizationRetryExecutor retryExecutor;

    public CatalogReconciliationRunner(
            ReconcileCatalogStateUseCase reconcileCatalog,
            SynchronizationRetryExecutor retryExecutor
    ) {
        this.reconcileCatalog = Objects.requireNonNull(reconcileCatalog);
        this.retryExecutor = Objects.requireNonNull(retryExecutor);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            retryExecutor.execute(Operation.RECONCILIATION, reconcileCatalog::reconcile);
        } catch (RuntimeException exception) {
            LOG.error(FAILURE_MESSAGE);
            throw new IllegalStateException(FAILURE_MESSAGE);
        }
    }
}
