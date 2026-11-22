package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogSnapshotTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Test
    void acceptsAConsistentSnapshotWithDisabledEntities() {
        CatalogSnapshot snapshot = validSnapshot();

        assertThat(snapshot.professionalCategories()).singleElement()
                .extracting(ProfessionalCategory::enabled)
                .isEqualTo(false);
        assertThat(snapshot.professionals()).singleElement()
                .extracting(Professional::enabled)
                .isEqualTo(false);
        assertThat(snapshot.weeklySchedules()).singleElement()
                .extracting(WeeklySchedule::enabled)
                .isEqualTo(false);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inconsistentSnapshots")
    void rejectsInconsistentSnapshot(
            String scenario,
            List<ProfessionalCategory> categories,
            List<Professional> professionals,
            List<WeeklySchedule> schedules
    ) {
        assertThatThrownBy(() -> new CatalogSnapshot(1L, UPDATED_AT, categories, professionals, schedules))
                .as(scenario)
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> inconsistentSnapshots() {
        var category = category(1L);
        return Stream.of(
                Arguments.of("duplicate category IDs", List.of(category, category), List.of(), List.of()),
                Arguments.of("absent category", List.of(), List.of(professional(99L)), List.of()),
                Arguments.of("absent professional", List.of(category), List.of(), List.of(schedule(99L)))
        );
    }

    @Test
    void rejectsNonPositiveSlotDuration() {
        var startTime = LocalTime.of(9, 0);
        var endTime = LocalTime.of(12, 0);
        assertThatThrownBy(() -> new WeeklySchedule(
                1L,
                1L,
                DayOfWeek.MONDAY,
                startTime,
                endTime,
                0,
                true,
                CREATED_AT,
                UPDATED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesImmutableCollections() {
        CatalogSnapshot snapshot = validSnapshot();
        var professionals = snapshot.professionals();
        var additionalProfessional = professional(1L);

        assertThatThrownBy(() -> professionals.add(additionalProfessional))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private CatalogSnapshot validSnapshot() {
        return new CatalogSnapshot(
                7L,
                UPDATED_AT,
                List.of(category(1L)),
                List.of(professional(1L)),
                List.of(schedule(15L))
        );
    }

    private static ProfessionalCategory category(long id) {
        return new ProfessionalCategory(
                id,
                "Clinical medicine",
                null,
                false,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static Professional professional(long categoryId) {
        return new Professional(
                15L,
                categoryId,
                "Ana",
                "Gomez",
                false,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static WeeklySchedule schedule(long professionalId) {
        return new WeeklySchedule(
                21L,
                professionalId,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                30,
                false,
                CREATED_AT,
                UPDATED_AT
        );
    }
}
