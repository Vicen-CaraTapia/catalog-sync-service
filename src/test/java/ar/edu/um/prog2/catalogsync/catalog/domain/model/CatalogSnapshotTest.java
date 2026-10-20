package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

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

    @Test
    void rejectsDuplicateCategoryIds() {
        ProfessionalCategory category = category(1L);

        assertThatThrownBy(() -> new CatalogSnapshot(
                1L,
                UPDATED_AT,
                List.of(category, category),
                List.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsProfessionalReferencingAnAbsentCategory() {
        assertThatThrownBy(() -> new CatalogSnapshot(
                1L,
                UPDATED_AT,
                List.of(),
                List.of(professional(99L)),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWeeklyScheduleReferencingAnAbsentProfessional() {
        assertThatThrownBy(() -> new CatalogSnapshot(
                1L,
                UPDATED_AT,
                List.of(category(1L)),
                List.of(),
                List.of(schedule(99L))
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveSlotDuration() {
        assertThatThrownBy(() -> new WeeklySchedule(
                1L,
                1L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                0,
                true,
                CREATED_AT,
                UPDATED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesImmutableCollections() {
        CatalogSnapshot snapshot = validSnapshot();

        assertThatThrownBy(() -> snapshot.professionals().add(professional(1L)))
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

    private ProfessionalCategory category(long id) {
        return new ProfessionalCategory(
                id,
                "Clinical medicine",
                null,
                false,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private Professional professional(long categoryId) {
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

    private WeeklySchedule schedule(long professionalId) {
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
