package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogPersistenceIT {

    private static final long CATEGORY_ID = 9_001L;
    private static final long PROFESSIONAL_ID = 9_002L;
    private static final long WEEKLY_SCHEDULE_ID = 9_003L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Autowired
    private ProfessionalCategoryJpaRepository professionalCategoryRepository;

    @Autowired
    private ProfessionalJpaRepository professionalRepository;

    @Autowired
    private WeeklyScheduleJpaRepository weeklyScheduleRepository;

    @Autowired
    private CatalogSyncStateRepository catalogSyncStateRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndRecoversCatalogEntitiesAndInitialSyncState() {
        professionalCategoryRepository.saveAndFlush(new ProfessionalCategoryEntity(
                CATEGORY_ID,
                "Clinical medicine",
                null,
                false,
                CREATED_AT,
                UPDATED_AT
        ));
        professionalRepository.saveAndFlush(new ProfessionalEntity(
                PROFESSIONAL_ID,
                CATEGORY_ID,
                "Ana",
                "Gomez",
                true,
                CREATED_AT,
                UPDATED_AT
        ));
        weeklyScheduleRepository.saveAndFlush(new WeeklyScheduleEntity(
                WEEKLY_SCHEDULE_ID,
                PROFESSIONAL_ID,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                30,
                true,
                CREATED_AT,
                UPDATED_AT
        ));

        entityManager.clear();

        ProfessionalCategoryEntity category = professionalCategoryRepository.findById(CATEGORY_ID).orElseThrow();
        ProfessionalEntity professional = professionalRepository.findById(PROFESSIONAL_ID).orElseThrow();
        WeeklyScheduleEntity weeklySchedule = weeklyScheduleRepository.findById(WEEKLY_SCHEDULE_ID).orElseThrow();
        CatalogSyncStateEntity syncState = catalogSyncStateRepository
                .findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow();

        assertThat(category.getDescription()).isNull();
        assertThat(category.isEnabled()).isFalse();
        assertThat(category.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(professional.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(professional.isEnabled()).isTrue();
        assertThat(weeklySchedule.getProfessionalId()).isEqualTo(PROFESSIONAL_ID);
        assertThat(weeklySchedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(weeklySchedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(weeklySchedule.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(weeklySchedule.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(syncState.getStateId()).isEqualTo(CatalogSyncStateEntity.SINGLETON_ID);
        assertThat(syncState.getAppliedVersion()).isNull();
    }
}
