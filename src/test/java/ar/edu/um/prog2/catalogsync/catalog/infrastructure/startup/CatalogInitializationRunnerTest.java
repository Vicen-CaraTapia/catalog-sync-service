package ar.edu.um.prog2.catalogsync.catalog.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.InitializeCatalogUseCase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CatalogInitializationRunnerTest {

    @Test
    void invokesInitializationOnce() {
        AtomicInteger calls = new AtomicInteger();
        InitializeCatalogUseCase useCase = calls::incrementAndGet;

        new CatalogInitializationRunner(useCase).run(new DefaultApplicationArguments());

        assertThat(calls).hasValue(1);
    }

    @Test
    void logsOnlyFixedMessageAndFailsWithoutExposingCause(CapturedOutput output) {
        RuntimeException failure = new IllegalStateException("secret-marker-must-not-be-logged");
        CatalogInitializationRunner runner = new CatalogInitializationRunner(() -> {
            throw failure;
        });

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Initial catalog synchronization failed; application startup aborted.")
                .hasNoCause();

        assertThat(output.getAll())
                .contains("Initial catalog synchronization failed; application startup aborted.")
                .doesNotContain("secret-marker-must-not-be-logged");
    }
}
