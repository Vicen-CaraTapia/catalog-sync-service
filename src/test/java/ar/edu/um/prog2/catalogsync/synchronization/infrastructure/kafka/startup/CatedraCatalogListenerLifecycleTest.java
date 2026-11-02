package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup.CatalogInitializationRunner;
import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ReconcileCatalogStateUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener.CatalogUpdatedListener;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.TransientSynchronizationFailureClassifier;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.startup.CatalogReconciliationRunner;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.listener.MessageListenerContainer;

@ExtendWith(OutputCaptureExtension.class)
class CatedraCatalogListenerLifecycleTest {

    @Test
    void realRegistryKeepsContainerStoppedUntilInitializationAndReconciliationComplete() {
        var fixture = new Fixture();
        doAnswer(invocation -> {
            assertThat(fixture.container.isRunning()).isFalse();
            verify(fixture.container, never()).start();
            return null;
        }).when(fixture.initialize).initializeIfNeeded();
        doAnswer(invocation -> {
            assertThat(fixture.container.isRunning()).isFalse();
            verify(fixture.container, never()).start();
            return null;
        }).when(fixture.reconcile).reconcile();

        try (var context = startup(fixture)) {
            assertThat(context.getBean(KafkaListenerEndpointRegistry.class)
                    .getListenerContainer(CatalogUpdatedListener.LISTENER_ID)).isSameAs(fixture.container);
            var order = inOrder(fixture.initialize, fixture.reconcile, fixture.appliedVersion, fixture.container);
            order.verify(fixture.initialize).initializeIfNeeded();
            order.verify(fixture.reconcile).reconcile();
            order.verify(fixture.appliedVersion).readAppliedVersion();
            order.verify(fixture.container).start();
            assertThat(fixture.container.isRunning()).isTrue();
        }
    }

    @Test
    void initializationFailurePreventsReconciliationAndKafkaStartup() {
        var fixture = new Fixture();
        doThrow(new IllegalStateException("Controlled initialization failure"))
                .when(fixture.initialize).initializeIfNeeded();

        assertThatThrownBy(() -> startup(fixture))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Initial catalog synchronization failed; application startup aborted.");

        verifyNoInteractions(fixture.reconcile, fixture.appliedVersion);
        verify(fixture.container, never()).start();
    }

    @Test
    void transientInitializationExhaustionPreventsKafkaStartup(CapturedOutput output) {
        var fixture = new Fixture();
        doThrow(new CatedraRedisUnavailableException(new SocketTimeoutException("private marker")))
                .when(fixture.initialize).initializeIfNeeded();

        assertThatThrownBy(() -> startup(fixture))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Initial catalog synchronization failed; application startup aborted.");

        verify(fixture.initialize, times(3)).initializeIfNeeded();
        verify(fixture.container, never()).start();
        assertThat(output.getAll()).doesNotContain("private marker");
    }

    @Test
    void reconciliationFailureAbortsStartupWithoutStartingKafkaOrLoggingTheCause(CapturedOutput output) {
        var fixture = new Fixture();
        doThrow(new IllegalStateException("secret-marker-must-not-be-logged"))
                .when(fixture.reconcile).reconcile();

        assertThatThrownBy(() -> startup(fixture))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Catalog state reconciliation failed; application startup aborted.");

        verify(fixture.initialize).initializeIfNeeded();
        verifyNoInteractions(fixture.appliedVersion);
        verify(fixture.container, never()).start();
        assertThat(output.getAll()).doesNotContain("secret-marker-must-not-be-logged");
    }

    private static ConfigurableApplicationContext startup(Fixture fixture) {
        var application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        application.setAdditionalProfiles("test");
        application.addInitializers(context -> {
            context.getBeanFactory().registerSingleton("startupFixture", fixture);
            context.getBeanFactory().registerSingleton("initializeCatalogUseCase", fixture.initialize);
            context.getBeanFactory().registerSingleton("reconcileCatalogStateUseCase", fixture.reconcile);
            context.getBeanFactory().registerSingleton("readAppliedCatalogVersionPort", fixture.appliedVersion);
        });
        return application.run("--catalog.initialization.enabled=true", "--catalog.reconciliation.enabled=true",
                "--catalog.incremental-consumer.enabled=true");
    }

    static class Fixture {
        final InitializeCatalogUseCase initialize = mock(InitializeCatalogUseCase.class);
        final ReconcileCatalogStateUseCase reconcile = mock(ReconcileCatalogStateUseCase.class);
        final ReadAppliedCatalogVersionPort appliedVersion = mock(ReadAppliedCatalogVersionPort.class);
        final MessageListenerContainer container = mock(MessageListenerContainer.class);

        Fixture() {
            var running = new AtomicBoolean();
            when(appliedVersion.readAppliedVersion()).thenReturn(7L);
            when(container.isAutoStartup()).thenReturn(false);
            when(container.isRunning()).thenAnswer(invocation -> running.get());
            doAnswer(invocation -> { running.set(true); return null; }).when(container).start();
            doAnswer(invocation -> {
                running.set(false);
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(container).stop(any(Runnable.class));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    // Reverse registration order deliberately: @Order, rather than bean registration, orders the runners.
    @Import({CatalogReconciliationRunner.class, CatedraCatalogListenerStarter.class, CatalogInitializationRunner.class,
            SynchronizationRetryExecutor.class, TransientSynchronizationFailureClassifier.class})
    static class TestApplication {

        @Bean
        CatalogSynchronizationRetryProperties catalogSynchronizationRetryProperties() {
            return new CatalogSynchronizationRetryProperties(3, Duration.ofMillis(1), Duration.ofMillis(2));
        }

        @Bean
        KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry(Fixture fixture) {
            var registry = new KafkaListenerEndpointRegistry();
            var endpoint = new MethodKafkaListenerEndpoint<String, String>();
            endpoint.setId(CatalogUpdatedListener.LISTENER_ID);
            @SuppressWarnings("unchecked")
            var factory = (KafkaListenerContainerFactory<MessageListenerContainer>) mock(KafkaListenerContainerFactory.class);
            when(factory.createListenerContainer(endpoint)).thenReturn(fixture.container);
            registry.registerListenerContainer(endpoint, factory);
            return registry;
        }
    }
}
