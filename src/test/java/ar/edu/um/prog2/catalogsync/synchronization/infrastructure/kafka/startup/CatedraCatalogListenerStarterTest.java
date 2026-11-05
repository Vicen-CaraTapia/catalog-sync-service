package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.startup;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener.CatalogUpdatedListener;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

class CatedraCatalogListenerStarterTest {

    private final KafkaListenerEndpointRegistry registry = mock(KafkaListenerEndpointRegistry.class);
    private final ReadAppliedCatalogVersionPort appliedVersionPort = mock(ReadAppliedCatalogVersionPort.class);
    private final CatedraCatalogListenerStarter starter =
            new CatedraCatalogListenerStarter(registry, appliedVersionPort);

    @Test
    void startsOnlyTheCatalogListenerAfterFindingAnAppliedVersion() {
        var container = mock(MessageListenerContainer.class);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(registry.getListenerContainer(CatalogUpdatedListener.LISTENER_ID)).thenReturn(container);

        starter.startCatalogListener();

        var order = inOrder(appliedVersionPort, registry, container);
        order.verify(appliedVersionPort).readAppliedVersion();
        order.verify(registry).getListenerContainer(CatalogUpdatedListener.LISTENER_ID);
        order.verify(container).start();
        order.verifyNoMoreInteractions();
    }

    @Test
    void doesNotStartWhenTheLocalCatalogIsUninitialized() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(null);

        assertThatThrownBy(starter::startCatalogListener)
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(registry);
    }

    @Test
    void failsIfTheDesignatedContainerIsMissing() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);

        assertThatThrownBy(starter::startCatalogListener)
                .isInstanceOf(IllegalStateException.class);
    }
}
