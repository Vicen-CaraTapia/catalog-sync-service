package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Requires a dedicated PostgreSQL database because applying a complete snapshot reconciles
 * the catalog tables as a whole. These tests intentionally do not run inside a test transaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplyCatalogSnapshotIT {

    private static final long FIRST_VERSION = 17L;
    private static final long REJECTED_VERSION = 999_999L;
    private static final String FAILURE_CONSTRAINT = "chk_test_reject_catalog_version";
    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Autowired
    private ApplyCatalogSnapshotUseCase applyCatalogSnapshot;

    @Autowired
    private ProfessionalCategoryJpaRepository categoryRepository;

    @Autowired
    private ProfessionalJpaRepository professionalRepository;

    @Autowired
    private WeeklyScheduleJpaRepository scheduleRepository;

    @Autowired
    private CatalogSyncStateRepository syncStateRepository;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void clearCatalogBeforeTest() {
        dropFailureConstraint();
        clearCatalog();
    }

    @AfterEach
    void clearCatalogAfterTest() {
        dropFailureConstraint();
        clearCatalog();
    }

    @Test
    void appliesCompleteSnapshotIncludingDisabledEntitiesAndVersion() {
        CatalogSnapshot snapshot = snapshot(
                20L,
                List.of(category(1L, "Clinical medicine", true), category(2L, "Dentistry", false)),
                List.of(professional(11L, 1L, "Ana", true), professional(12L, 2L, "Luis", false)),
                List.of(schedule(101L, 11L, DayOfWeek.MONDAY, true),
                        schedule(102L, 12L, DayOfWeek.TUESDAY, false))
        );

        applyCatalogSnapshot.apply(snapshot);

        assertThat(readPersistedState()).isEqualTo(persistedState(snapshot));
    }

    @Test
    void updatesAndInsertsRecordsRemovesAbsentRecordsAndMovesProfessionalBeforeCategoryRemoval() {
        CatalogSnapshot initial = snapshot(
                FIRST_VERSION,
                List.of(category(1L, "Old category", true), category(2L, "Existing category", true)),
                List.of(professional(11L, 1L, "Ana", true), professional(12L, 2L, "Luis", true)),
                List.of(schedule(101L, 11L, DayOfWeek.MONDAY, true),
                        schedule(102L, 12L, DayOfWeek.TUESDAY, true))
        );
        applyCatalogSnapshot.apply(initial);

        CatalogSnapshot updated = snapshot(
                18L,
                List.of(category(2L, "Updated category", false), category(3L, "New category", true)),
                List.of(professional(11L, 2L, "Ana updated", false), professional(13L, 3L, "Marta", true)),
                List.of(schedule(101L, 11L, DayOfWeek.WEDNESDAY, false),
                        schedule(103L, 13L, DayOfWeek.FRIDAY, true))
        );

        applyCatalogSnapshot.apply(updated);

        assertThat(readPersistedState()).isEqualTo(persistedState(updated));
        assertThat(categoryRepository.findById(1L)).isEmpty();
        assertThat(professionalRepository.findById(12L)).isEmpty();
        assertThat(scheduleRepository.findById(102L)).isEmpty();
        assertThat(professionalRepository.findById(11L).orElseThrow().getCategoryId()).isEqualTo(2L);
    }

    @Test
    void applyingSameSnapshotTwiceIsFunctionallyIdempotent() {
        CatalogSnapshot snapshot = snapshot(
                22L,
                List.of(category(1L, "Clinical medicine", true)),
                List.of(professional(11L, 1L, "Ana", true)),
                List.of(schedule(101L, 11L, DayOfWeek.MONDAY, true))
        );

        applyCatalogSnapshot.apply(snapshot);
        PersistedCatalogState afterFirstApplication = readPersistedState();

        applyCatalogSnapshot.apply(snapshot);

        assertThat(readPersistedState()).isEqualTo(afterFirstApplication);
    }

    @Test
    void rollsBackCatalogChangesAndVersionWhenPostgresRejectsVersionUpdate() {
        CatalogSnapshot original = snapshot(
                FIRST_VERSION,
                List.of(category(1L, "Original category", true)),
                List.of(professional(11L, 1L, "Original professional", true)),
                List.of(schedule(101L, 11L, DayOfWeek.MONDAY, true))
        );
        applyCatalogSnapshot.apply(original);
        PersistedCatalogState beforeFailure = readPersistedState();
        jdbcTemplate.execute("ALTER TABLE catalog_sync_state ADD CONSTRAINT " + FAILURE_CONSTRAINT
                + " CHECK (applied_version IS DISTINCT FROM " + REJECTED_VERSION + ")");

        CatalogSnapshot rejected = snapshot(
                REJECTED_VERSION,
                List.of(category(2L, "Replacement category", false)),
                List.of(professional(22L, 2L, "Replacement professional", false)),
                List.of(schedule(202L, 22L, DayOfWeek.FRIDAY, false))
        );

        assertThatThrownBy(() -> applyCatalogSnapshot.apply(rejected))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(readPersistedState()).isEqualTo(beforeFailure);
    }

    private void clearCatalog() {
        scheduleRepository.deleteAll();
        scheduleRepository.flush();
        professionalRepository.deleteAll();
        professionalRepository.flush();
        categoryRepository.deleteAll();
        categoryRepository.flush();
        jdbcTemplate.update("UPDATE catalog_sync_state SET applied_version = NULL WHERE state_id = ?",
                CatalogSyncStateEntity.SINGLETON_ID);
    }

    private void dropFailureConstraint() {
        jdbcTemplate.execute("ALTER TABLE catalog_sync_state DROP CONSTRAINT IF EXISTS " + FAILURE_CONSTRAINT);
    }

    private PersistedCatalogState readPersistedState() {
        List<CategoryRow> categories = categoryRepository.findAll().stream()
                .map(category -> new CategoryRow(category.getId(), category.getName(), category.getDescription(),
                        category.isEnabled(), category.getCreatedAt(), category.getUpdatedAt()))
                .sorted(Comparator.comparing(CategoryRow::id))
                .toList();
        List<ProfessionalRow> professionals = professionalRepository.findAll().stream()
                .map(professional -> new ProfessionalRow(professional.getId(), professional.getCategoryId(),
                        professional.getFirstName(), professional.getLastName(), professional.isEnabled(),
                        professional.getCreatedAt(), professional.getUpdatedAt()))
                .sorted(Comparator.comparing(ProfessionalRow::id))
                .toList();
        List<ScheduleRow> schedules = scheduleRepository.findAll().stream()
                .map(schedule -> new ScheduleRow(schedule.getId(), schedule.getProfessionalId(),
                        schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(),
                        schedule.getSlotDurationMinutes(), schedule.isEnabled(), schedule.getCreatedAt(),
                        schedule.getUpdatedAt()))
                .sorted(Comparator.comparing(ScheduleRow::id))
                .toList();
        Long appliedVersion = syncStateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow()
                .getAppliedVersion();

        return new PersistedCatalogState(categories, professionals, schedules, appliedVersion);
    }

    private static PersistedCatalogState persistedState(CatalogSnapshot snapshot) {
        List<CategoryRow> categories = snapshot.professionalCategories().stream()
                .map(category -> new CategoryRow(category.id(), category.name(), category.description(),
                        category.enabled(), category.createdAt(), category.updatedAt()))
                .sorted(Comparator.comparing(CategoryRow::id))
                .toList();
        List<ProfessionalRow> professionals = snapshot.professionals().stream()
                .map(professional -> new ProfessionalRow(professional.id(), professional.categoryId(),
                        professional.firstName(), professional.lastName(), professional.enabled(),
                        professional.createdAt(), professional.updatedAt()))
                .sorted(Comparator.comparing(ProfessionalRow::id))
                .toList();
        List<ScheduleRow> schedules = snapshot.weeklySchedules().stream()
                .map(schedule -> new ScheduleRow(schedule.id(), schedule.professionalId(), schedule.dayOfWeek(),
                        schedule.startTime(), schedule.endTime(), schedule.slotDurationMinutes(),
                        schedule.enabled(), schedule.createdAt(), schedule.updatedAt()))
                .sorted(Comparator.comparing(ScheduleRow::id))
                .toList();

        return new PersistedCatalogState(categories, professionals, schedules, snapshot.version());
    }

    private static CatalogSnapshot snapshot(
            long version,
            List<ProfessionalCategory> categories,
            List<Professional> professionals,
            List<WeeklySchedule> schedules
    ) {
        return new CatalogSnapshot(version, UPDATED_AT, categories, professionals, schedules);
    }

    private static ProfessionalCategory category(long id, String name, boolean enabled) {
        return new ProfessionalCategory(id, name, null, enabled, CREATED_AT, UPDATED_AT);
    }

    private static Professional professional(long id, long categoryId, String firstName, boolean enabled) {
        return new Professional(id, categoryId, firstName, "Surname", enabled, CREATED_AT, UPDATED_AT);
    }

    private static WeeklySchedule schedule(long id, long professionalId, DayOfWeek day, boolean enabled) {
        return new WeeklySchedule(id, professionalId, day, LocalTime.of(9, 0), LocalTime.of(12, 0),
                30, enabled, CREATED_AT, UPDATED_AT);
    }

    private record PersistedCatalogState(
            List<CategoryRow> categories,
            List<ProfessionalRow> professionals,
            List<ScheduleRow> schedules,
            Long appliedVersion
    ) {
    }

    private record CategoryRow(
            Long id,
            String name,
            String description,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    private record ProfessionalRow(
            Long id,
            Long categoryId,
            String firstName,
            String lastName,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    private record ScheduleRow(
            Long id,
            Long professionalId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
