package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class CatalogPersistenceEntitiesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Test
    void preservesProfessionalCategoryFields() {
        var category = new ProfessionalCategoryEntity(
                1L,
                "Clinical medicine",
                null,
                false,
                CREATED_AT,
                UPDATED_AT
        );

        assertThat(category.getId()).isEqualTo(1L);
        assertThat(category.getName()).isEqualTo("Clinical medicine");
        assertThat(category.getDescription()).isNull();
        assertThat(category.isEnabled()).isFalse();
        assertThat(category.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(category.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void preservesProfessionalFields() {
        var professional = new ProfessionalEntity(
                15L,
                1L,
                "Ana",
                "Gomez",
                true,
                CREATED_AT,
                UPDATED_AT
        );

        assertThat(professional.getId()).isEqualTo(15L);
        assertThat(professional.getCategoryId()).isEqualTo(1L);
        assertThat(professional.getFirstName()).isEqualTo("Ana");
        assertThat(professional.getLastName()).isEqualTo("Gomez");
        assertThat(professional.isEnabled()).isTrue();
        assertThat(professional.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(professional.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void preservesWeeklyScheduleFields() {
        var schedule = new WeeklyScheduleEntity(
                21L,
                15L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                30,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        assertThat(schedule.getId()).isEqualTo(21L);
        assertThat(schedule.getProfessionalId()).isEqualTo(15L);
        assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(schedule.isEnabled()).isTrue();
        assertThat(schedule.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(schedule.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void createsSingletonCatalogSyncState() {
        var state = new CatalogSyncStateEntity(null);

        assertThat(state.getStateId()).isEqualTo(CatalogSyncStateEntity.SINGLETON_ID);
        assertThat(state.getAppliedVersion()).isNull();
    }
}
