package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CatalogReconciliationRunnerTest {

    @Test
    void invokesReconciliationOnce() {
        var calls = new AtomicInteger();

        new CatalogReconciliationRunner(calls::incrementAndGet).run(new DefaultApplicationArguments());

        assertThat(calls).hasValue(1);
    }

    @Test
    void propagatesOnlyFixedFailureWithoutLoggingSensitiveCause(CapturedOutput output) {
        var runner = new CatalogReconciliationRunner(() -> {
            throw new IllegalStateException("secret-marker-must-not-be-logged");
        });

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Catalog state reconciliation failed; application startup aborted.")
                .hasNoCause();
        assertThat(output.getAll())
                .contains("Catalog state reconciliation failed; application startup aborted.")
                .doesNotContain("secret-marker-must-not-be-logged");
    }
}
