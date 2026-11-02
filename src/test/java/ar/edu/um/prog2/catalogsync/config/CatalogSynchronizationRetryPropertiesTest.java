package ar.edu.um.prog2.catalogsync.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

class CatalogSynchronizationRetryPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsApprovedDefaultsWithoutRequiringExternalValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            var properties = context.getBean(CatalogSynchronizationRetryProperties.class);
            assertThat(properties.maxAttempts()).isEqualTo(3);
            assertThat(properties.firstBackoff()).isEqualTo(Duration.ofSeconds(1));
            assertThat(properties.secondBackoff()).isEqualTo(Duration.ofSeconds(2));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void acceptsValidReducedAttemptBudgets(int attempts) {
        contextRunner.withPropertyValues("catalog.synchronization.retry.max-attempts=" + attempts)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CatalogSynchronizationRetryProperties.class).maxAttempts())
                            .isEqualTo(attempts);
                });
    }

    @Test
    void acceptsExternalShorterBackoffsWithinApprovedBounds() {
        contextRunner.withPropertyValues("catalog.synchronization.retry.first-backoff=250ms",
                        "catalog.synchronization.retry.second-backoff=500ms")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(CatalogSynchronizationRetryProperties.class);
                    assertThat(properties.firstBackoff()).isEqualTo(Duration.ofMillis(250));
                    assertThat(properties.secondBackoff()).isEqualTo(Duration.ofMillis(500));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-attempts=0", "max-attempts=-1", "max-attempts=4",
            "first-backoff=0s", "first-backoff=-1s", "first-backoff=1001ms",
            "second-backoff=0s", "second-backoff=-1s", "second-backoff=2001ms",
            "second-backoff=500ms", "first-backoff=invalid"})
    void invalidExternalConfigurationPreventsContextStartup(String value) {
        contextRunner.withPropertyValues("catalog.synchronization.retry." + value).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).isInstanceOf(ConfigurationPropertiesBindException.class);
        });
    }

    @Test
    void directConstructionCannotBypassTheBudgetOrNullDelayValidation() {
        assertThatThrownBy(() -> new CatalogSynchronizationRetryProperties(4,
                Duration.ofSeconds(1), Duration.ofSeconds(2))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CatalogSynchronizationRetryProperties(3, null, Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CatalogSynchronizationRetryProperties(3, Duration.ofSeconds(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CatalogSynchronizationRetryProperties.class)
    static class PropertiesConfiguration {
    }
}
