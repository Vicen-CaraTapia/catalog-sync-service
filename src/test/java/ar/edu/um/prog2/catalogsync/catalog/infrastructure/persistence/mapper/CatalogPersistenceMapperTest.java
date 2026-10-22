package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void mapsCategoryDomainModelToPersistenceEntity() {
        ProfessionalCategory category = new ProfessionalCategory(
                7L, "Cardiology", "Heart care", false, CREATED_AT, UPDATED_AT
        );

        ProfessionalCategoryEntity entity = CatalogPersistenceMapper.toEntity(category);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo("Cardiology");
        assertThat(entity.getDescription()).isEqualTo("Heart care");
        assertThat(entity.isEnabled()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void mapsProfessionalDomainModelToPersistenceEntity() {
        Professional professional = professional(13L, 7L, "Ana", "Gomez", false);

        ProfessionalEntity entity = CatalogPersistenceMapper.toEntity(professional);

        assertThat(entity.getId()).isEqualTo(13L);
        assertThat(entity.getCategoryId()).isEqualTo(7L);
        assertThat(entity.getFirstName()).isEqualTo("Ana");
        assertThat(entity.getLastName()).isEqualTo("Gomez");
        assertThat(entity.isEnabled()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void mapsProfessionalPersistenceEntityToDomainModel() {
        ProfessionalEntity entity = new ProfessionalEntity(
                13L, 7L, "Ana", "Gomez", false, CREATED_AT, UPDATED_AT
        );

        assertThat(CatalogPersistenceMapper.toDomain(entity))
                .isEqualTo(professional(13L, 7L, "Ana", "Gomez", false));
    }

    @Test
    void mapsWeeklyScheduleDomainModelToPersistenceEntity() {
        WeeklySchedule schedule = new WeeklySchedule(
                29L,
                13L,
                DayOfWeek.THURSDAY,
                LocalTime.of(9, 30),
                LocalTime.of(13, 0),
                30,
                false,
                CREATED_AT,
                UPDATED_AT
        );

        WeeklyScheduleEntity entity = CatalogPersistenceMapper.toEntity(schedule);

        assertThat(entity.getId()).isEqualTo(29L);
        assertThat(entity.getProfessionalId()).isEqualTo(13L);
        assertThat(entity.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(entity.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(entity.isEnabled()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    private static Professional professional(
            long id,
            long categoryId,
            String firstName,
            String lastName,
            boolean enabled
    ) {
        return new Professional(id, categoryId, firstName, lastName, enabled, CREATED_AT, UPDATED_AT);
    }
}
