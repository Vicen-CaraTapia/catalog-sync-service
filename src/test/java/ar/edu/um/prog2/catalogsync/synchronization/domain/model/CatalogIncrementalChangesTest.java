package ar.edu.um.prog2.catalogsync.synchronization.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import org.junit.jupiter.api.Test;

class CatalogIncrementalChangesTest {

    @Test
    void acceptsPartialAffectedCollectionsAndCopiesTheirContents() {
        var professionals = new ArrayList<>(List.of(new Professional(
                15, 1, "Ana", "Gomez", false,
                Instant.parse("2026-07-01T12:00:00Z"),
                Instant.parse("2026-09-10T13:40:00Z")
        )));

        var changes = new CatalogIncrementalChanges(7, List.of(), professionals, List.of());
        professionals.clear();

        assertThat(changes.requestedChangeVersion()).isEqualTo(7);
        assertThat(changes.currentProfessionalCategories()).isEmpty();
        assertThat(changes.currentProfessionals()).extracting(Professional::id).containsExactly(15L);
        assertThat(changes.currentProfessionals().getFirst().enabled()).isFalse();
        assertThat(changes.currentWeeklySchedules()).isEmpty();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> changes.currentProfessionals().clear());
    }

    @Test
    void rejectsMissingOrNullCollectionContents() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CatalogIncrementalChanges(7, null, List.of(), List.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CatalogIncrementalChanges(
                        7, List.of(), new ArrayList<>(java.util.Arrays.asList((Professional) null)), List.of()
                ));
    }
}
