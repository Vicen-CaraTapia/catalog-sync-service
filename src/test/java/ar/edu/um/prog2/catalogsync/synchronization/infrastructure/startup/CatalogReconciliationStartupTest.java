package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ReconcileCatalogStateUseCase;
import ar.edu.um.prog2.catalogsync.config.CatalogSynchronizationRetryProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.SynchronizationRetryExecutor;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.retry.TransientSynchronizationFailureClassifier;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

class CatalogReconciliationStartupTest {

    @Test
    void reconciliationIsEnabledByDefault() {
        var reconcile = mock(ReconcileCatalogStateUseCase.class);

        try (var context = application(reconcile).run()) {
            assertThat(context.getBeansOfType(CatalogReconciliationRunner.class)).hasSize(1);
            verify(reconcile).reconcile();
        }
    }

    @Test
    void explicitFalseDisablesTheRunner() {
        var reconcile = mock(ReconcileCatalogStateUseCase.class);

        try (var context = application(reconcile).run("--catalog.reconciliation.enabled=false")) {
            assertThat(context.getBeansOfType(CatalogReconciliationRunner.class)).isEmpty();
            verifyNoInteractions(reconcile);
        }
    }

    @Test
    void ordinaryTestProfileDoesNotRunReconciliation() {
        var reconcile = mock(ReconcileCatalogStateUseCase.class);
        var application = application(reconcile);
        application.setAdditionalProfiles("test");

        try (var context = application.run()) {
            assertThat(context.getBeansOfType(CatalogReconciliationRunner.class)).isEmpty();
            verifyNoInteractions(reconcile);
        }
    }

    @Test
    void startupTestsCanExplicitlyEnableReconciliationOverTheTestProfile() {
        var reconcile = mock(ReconcileCatalogStateUseCase.class);
        var application = application(reconcile);
        application.setAdditionalProfiles("test");

        try (var context = application.run("--catalog.reconciliation.enabled=true")) {
            assertThat(context.getBeansOfType(CatalogReconciliationRunner.class)).hasSize(1);
            verify(reconcile).reconcile();
        }
    }

    private static SpringApplication application(ReconcileCatalogStateUseCase reconcile) {
        var application = new SpringApplication(RunnerConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        application.addInitializers(context ->
                context.getBeanFactory().registerSingleton("reconcileCatalogStateUseCase", reconcile));
        return application;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({CatalogReconciliationRunner.class, SynchronizationRetryExecutor.class,
            TransientSynchronizationFailureClassifier.class})
    static class RunnerConfiguration {

        @Bean
        CatalogSynchronizationRetryProperties catalogSynchronizationRetryProperties() {
            return new CatalogSynchronizationRetryProperties(3,
                    Duration.ofSeconds(1), Duration.ofSeconds(2));
        }
    }
}
