package ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ExtendWith(OutputCaptureExtension.class)
class CatalogInitializationStartupTest {

    @Test
    void failedInitializationAbortsApplicationStartupWithoutLoggingCause(CapturedOutput output) {
        assertThatThrownBy(() -> application().run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Initial catalog synchronization failed; application startup aborted.");

        assertThat(output.getAll())
                .contains("Initial catalog synchronization failed; application startup aborted.")
                .doesNotContain("secret-marker-must-not-be-logged");
    }

    @Test
    void testProfileDoesNotRunInitialization() {
        SpringApplication application = application();
        application.setAdditionalProfiles("test");

        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getBeanNamesForType(CatalogInitializationRunner.class)).isEmpty();
        }
    }

    private static SpringApplication application() {
        SpringApplication application = new SpringApplication(FailingInitializationConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        return application;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(CatalogInitializationRunner.class)
    static class FailingInitializationConfiguration {

        @Bean
        InitializeCatalogUseCase initializeCatalogUseCase() {
            return () -> {
                throw new IllegalStateException("secret-marker-must-not-be-logged");
            };
        }
    }
}
