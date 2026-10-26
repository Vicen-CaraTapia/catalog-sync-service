package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.startup;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener.CatalogUpdatedListener;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "catalog.incremental-consumer",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public final class CatedraCatalogListenerStarter {

    private final KafkaListenerEndpointRegistry registry;
    private final ReadAppliedCatalogVersionPort appliedVersionPort;

    public CatedraCatalogListenerStarter(
            KafkaListenerEndpointRegistry registry,
            ReadAppliedCatalogVersionPort appliedVersionPort
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.appliedVersionPort = Objects.requireNonNull(appliedVersionPort);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startCatalogListener() {
        if (appliedVersionPort.readAppliedVersion() == null) {
            throw new IllegalStateException("The local catalog has not been initialized");
        }

        var container = registry.getListenerContainer(CatalogUpdatedListener.LISTENER_ID);
        if (container == null) {
            throw new IllegalStateException("Catalog update listener container is missing");
        }
        container.start();
    }
}
