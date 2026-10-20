package ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
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

    public CatalogInitializationRunner(InitializeCatalogUseCase initializeCatalogUseCase) {
        this.initializeCatalogUseCase = Objects.requireNonNull(initializeCatalogUseCase);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializeCatalogUseCase.initializeIfNeeded();
        }
        catch (RuntimeException exception) {
            LOG.error(FAILURE_MESSAGE);
            throw new IllegalStateException(FAILURE_MESSAGE);
        }
    }
}
