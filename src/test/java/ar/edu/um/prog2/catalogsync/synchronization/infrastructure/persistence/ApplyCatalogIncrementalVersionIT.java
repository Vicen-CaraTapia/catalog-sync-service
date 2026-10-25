package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Uses real commits and rollbacks. Run only against a dedicated PostgreSQL test database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplyCatalogIncrementalVersionIT {

    private static final long CATEGORY = -531_001L;
    private static final long UNCHANGED_CATEGORY = -531_002L;
    private static final long NEW_CATEGORY = -531_003L;
    private static final long PROFESSIONAL = -532_001L;
    private static final long UNCHANGED_PROFESSIONAL = -532_002L;
    private static final long NEW_PROFESSIONAL = -532_003L;
    private static final long SCHEDULE = -533_001L;
    private static final long UNCHANGED_SCHEDULE = -533_002L;
    private static final long NEW_SCHEDULE = -533_003L;
    private static final Instant CREATED = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-09-02T00:00:00Z");

    @Autowired
    private ApplyCatalogIncrementalVersionUseCase applyVersion;
    @Autowired
    private CatalogSyncStateRepository stateRepository;
    @Autowired
    private ProfessionalCategoryJpaRepository categoryRepository;
    @Autowired
    private ProfessionalJpaRepository professionalRepository;
    @Autowired
    private WeeklyScheduleJpaRepository scheduleRepository;

    private JdbcTemplate jdbc;
    private Long originalVersion;
    private boolean restoreRequired;

    @Autowired
    void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void seedCatalog() {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        if (database == null || !database.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException("Incremental integration tests require a dedicated test database");
        }
        originalVersion = appliedVersion();
        restoreRequired = true;
        removeFixtures();
        jdbc.update("UPDATE catalog_sync_state SET applied_version = 6 WHERE state_id = 1");
        categoryRepository.saveAll(List.of(
                new ProfessionalCategoryEntity(CATEGORY, "Original", null, true, CREATED, CREATED),
                new ProfessionalCategoryEntity(UNCHANGED_CATEGORY, "Unchanged", null, true, CREATED, CREATED)));
        professionalRepository.saveAll(List.of(
                new ProfessionalEntity(PROFESSIONAL, CATEGORY, "Ana", "One", true, CREATED, CREATED),
                new ProfessionalEntity(UNCHANGED_PROFESSIONAL, UNCHANGED_CATEGORY,
                        "Luis", "Two", true, CREATED, CREATED)));
        scheduleRepository.saveAll(List.of(
                new WeeklyScheduleEntity(SCHEDULE, PROFESSIONAL, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, CREATED, CREATED),
                new WeeklyScheduleEntity(UNCHANGED_SCHEDULE, UNCHANGED_PROFESSIONAL, DayOfWeek.TUESDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, CREATED, CREATED)));
    }

    @AfterEach
    void restoreCatalog() {
        if (restoreRequired) {
            removeFixtures();
            jdbc.update("UPDATE catalog_sync_state SET applied_version = ? WHERE state_id = 1", originalVersion);
        }
    }

    @Test
    void updatesAndInsertsOnlyAffectedEntitiesAndCommitsVersion() {
        applyVersion.apply(new CatalogIncrementalChanges(7,
                List.of(category(CATEGORY, "Updated", false), category(NEW_CATEGORY, "New", true)),
                List.of(professional(PROFESSIONAL, NEW_CATEGORY, "Ana updated", false),
                        professional(NEW_PROFESSIONAL, NEW_CATEGORY, "Marta", true)),
                List.of(schedule(SCHEDULE, PROFESSIONAL, false),
                        schedule(NEW_SCHEDULE, NEW_PROFESSIONAL, true))));

        assertThat(appliedVersion()).isEqualTo(7L);
        assertThat(categoryRepository.findById(CATEGORY).orElseThrow().isEnabled()).isFalse();
        assertThat(categoryRepository.findById(NEW_CATEGORY)).isPresent();
        assertThat(professionalRepository.findById(PROFESSIONAL).orElseThrow().getCategoryId())
                .isEqualTo(NEW_CATEGORY);
        assertThat(professionalRepository.findById(PROFESSIONAL).orElseThrow().isEnabled()).isFalse();
        assertThat(professionalRepository.findById(NEW_PROFESSIONAL)).isPresent();
        assertThat(scheduleRepository.findById(SCHEDULE).orElseThrow().isEnabled()).isFalse();
        assertThat(scheduleRepository.findById(NEW_SCHEDULE)).isPresent();
        assertThat(categoryRepository.findById(UNCHANGED_CATEGORY).orElseThrow().getName())
                .isEqualTo("Unchanged");
        assertThat(professionalRepository.findById(UNCHANGED_PROFESSIONAL).orElseThrow().getFirstName())
                .isEqualTo("Luis");
        assertThat(scheduleRepository.findById(UNCHANGED_SCHEDULE).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void rejectsOutOfOrderVersionWithoutChangingData() {
        assertThatThrownBy(() -> applyVersion.apply(new CatalogIncrementalChanges(8,
                List.of(category(CATEGORY, "Rejected", false)), List.of(), List.of())))
                .isInstanceOf(IllegalStateException.class);

        assertThat(appliedVersion()).isEqualTo(6L);
        assertThat(categoryRepository.findById(CATEGORY).orElseThrow().getName()).isEqualTo("Original");
    }

    @Test
    void rollsBackAllAffectedEntitiesWhenPostgresRejectsTheLastCollection() {
        var changes = new CatalogIncrementalChanges(7,
                List.of(category(CATEGORY, "Should roll back", false)),
                List.of(professional(PROFESSIONAL, CATEGORY, "Should roll back", false)),
                List.of(schedule(NEW_SCHEDULE, -999_999_999L, true)));

        assertThatThrownBy(() -> applyVersion.apply(changes)).isInstanceOf(RuntimeException.class);

        assertThat(appliedVersion()).isEqualTo(6L);
        assertThat(categoryRepository.findById(CATEGORY).orElseThrow().getName()).isEqualTo("Original");
        assertThat(categoryRepository.findById(CATEGORY).orElseThrow().isEnabled()).isTrue();
        assertThat(professionalRepository.findById(PROFESSIONAL).orElseThrow().getFirstName())
                .isEqualTo("Ana");
        assertThat(professionalRepository.findById(PROFESSIONAL).orElseThrow().isEnabled()).isTrue();
        assertThat(scheduleRepository.findById(NEW_SCHEDULE)).isEmpty();
    }

    private Long appliedVersion() {
        return stateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow().getAppliedVersion();
    }

    private void removeFixtures() {
        jdbc.update("DELETE FROM weekly_schedules WHERE id IN (?, ?, ?)",
                SCHEDULE, UNCHANGED_SCHEDULE, NEW_SCHEDULE);
        jdbc.update("DELETE FROM professionals WHERE id IN (?, ?, ?)",
                PROFESSIONAL, UNCHANGED_PROFESSIONAL, NEW_PROFESSIONAL);
        jdbc.update("DELETE FROM professional_categories WHERE id IN (?, ?, ?)",
                CATEGORY, UNCHANGED_CATEGORY, NEW_CATEGORY);
    }

    private static ProfessionalCategory category(long id, String name, boolean enabled) {
        return new ProfessionalCategory(id, name, null, enabled, CREATED, UPDATED);
    }

    private static Professional professional(long id, long categoryId, String firstName, boolean enabled) {
        return new Professional(id, categoryId, firstName, "Surname", enabled, CREATED, UPDATED);
    }

    private static WeeklySchedule schedule(long id, long professionalId, boolean enabled) {
        return new WeeklySchedule(id, professionalId, DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0), LocalTime.of(13, 0), 30, enabled, CREATED, UPDATED);
    }
}
