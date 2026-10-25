package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.startup;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup.CatalogInitializationRunner;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener.CatalogUpdatedListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

class CatedraCatalogListenerLifecycleTest {

    @Test
    void startsTheCatalogListenerOnlyAfterCatalogInitializationCompletes() {
        var initialize = mock(InitializeCatalogUseCase.class);
        var appliedVersion = mock(ReadAppliedCatalogVersionPort.class);
        var registry = mock(KafkaListenerEndpointRegistry.class);
        var container = mock(MessageListenerContainer.class);
        when(appliedVersion.readAppliedVersion()).thenReturn(7L);
        when(registry.getListenerContainer(CatalogUpdatedListener.LISTENER_ID)).thenReturn(container);

        try (var context = application(initialize, appliedVersion, registry).run()) {
            var order = inOrder(initialize, appliedVersion, registry, container);
            order.verify(initialize).initializeIfNeeded();
            order.verify(appliedVersion).readAppliedVersion();
            order.verify(registry).getListenerContainer(CatalogUpdatedListener.LISTENER_ID);
            order.verify(container).start();
        }
    }

    @Test
    void doesNotStartKafkaWhenCatalogInitializationFails() {
        var initialize = mock(InitializeCatalogUseCase.class);
        var appliedVersion = mock(ReadAppliedCatalogVersionPort.class);
        var registry = mock(KafkaListenerEndpointRegistry.class);
        doThrow(new IllegalStateException("Controlled initialization failure"))
                .when(initialize).initializeIfNeeded();

        assertThatThrownBy(() -> application(initialize, appliedVersion, registry).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Initial catalog synchronization failed; application startup aborted.");

        verify(registry, never()).getListenerContainer(CatalogUpdatedListener.LISTENER_ID);
        verify(appliedVersion, never()).readAppliedVersion();
    }

    private static SpringApplication application(
            InitializeCatalogUseCase initialize,
            ReadAppliedCatalogVersionPort appliedVersion,
            KafkaListenerEndpointRegistry registry
    ) {
        var application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        application.addInitializers(context -> {
            context.getBeanFactory().registerSingleton("initializeCatalogUseCase", initialize);
            context.getBeanFactory().registerSingleton("readAppliedCatalogVersionPort", appliedVersion);
            context.getBeanFactory().registerSingleton("kafkaListenerEndpointRegistry", registry);
        });
        return application;
    }

    @SpringBootConfiguration
    @Import({CatalogInitializationRunner.class, CatedraCatalogListenerStarter.class})
    static class TestApplication {
    }
}
