package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.GetProfessionalDetailsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.LoadProfessionalDetailsPort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.TransactionTemplate;

/** Runs only with the postgres-integration profile against real PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class JpaProfessionalDetailsAdapterIT {

    private static final long CATEGORY_ID = -42_001L;
    private static final long PROFESSIONAL_ID = -42_101L;
    private static final long OTHER_PROFESSIONAL_ID = -42_102L;
    private static final long ENABLED_SCHEDULE_ID = -42_202L;
    private static final long DISABLED_SCHEDULE_ID = -42_201L;
    private static final long OTHER_SCHEDULE_ID = -42_203L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Autowired
    private GetProfessionalDetailsUseCase getProfessionalDetails;

    @Autowired
    private LoadProfessionalDetailsPort detailsPort;

    @Autowired
    private ProfessionalCategoryJpaRepository categoryRepository;

    @Autowired
    private ProfessionalJpaRepository professionalRepository;

    @Autowired
    private WeeklyScheduleJpaRepository scheduleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void loadsCategoryAndOnlyRequestedProfessionalsSchedulesIncludingDisabledOnes() {
        // Never replace or delete data that existed before this test.
        assertThat(categoryRepository.existsById(CATEGORY_ID)).isFalse();
        assertThat(professionalRepository.existsById(PROFESSIONAL_ID)).isFalse();
        assertThat(professionalRepository.existsById(OTHER_PROFESSIONAL_ID)).isFalse();
        assertThat(scheduleRepository.existsById(ENABLED_SCHEDULE_ID)).isFalse();
        assertThat(scheduleRepository.existsById(DISABLED_SCHEDULE_ID)).isFalse();
        assertThat(scheduleRepository.existsById(OTHER_SCHEDULE_ID)).isFalse();

        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        try {
            transactions.executeWithoutResult(status -> {
                categoryRepository.saveAndFlush(new ProfessionalCategoryEntity(
                        CATEGORY_ID, "C4.2 test category", null, true, CREATED_AT, UPDATED_AT
                ));
                professionalRepository.saveAllAndFlush(List.of(
                        new ProfessionalEntity(PROFESSIONAL_ID, CATEGORY_ID,
                                "Ana", "Gomez", false, CREATED_AT, UPDATED_AT),
                        new ProfessionalEntity(OTHER_PROFESSIONAL_ID, CATEGORY_ID,
                                "Luis", "Perez", true, CREATED_AT, UPDATED_AT)
                ));
                scheduleRepository.saveAllAndFlush(List.of(
                        new WeeklyScheduleEntity(ENABLED_SCHEDULE_ID, PROFESSIONAL_ID,
                                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0),
                                30, true, CREATED_AT, UPDATED_AT),
                        new WeeklyScheduleEntity(DISABLED_SCHEDULE_ID, PROFESSIONAL_ID,
                                DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(13, 0),
                                20, false, CREATED_AT, UPDATED_AT),
                        new WeeklyScheduleEntity(OTHER_SCHEDULE_ID, OTHER_PROFESSIONAL_ID,
                                DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(11, 0),
                                30, true, CREATED_AT, UPDATED_AT)
                ));
            });

            ProfessionalDetails details = getProfessionalDetails.getById(PROFESSIONAL_ID);

            assertThat(details.professional()).isEqualTo(new Professional(
                    PROFESSIONAL_ID, CATEGORY_ID, "Ana", "Gomez", false, CREATED_AT, UPDATED_AT
            ));
            assertThat(details.category()).isEqualTo(new ProfessionalCategory(
                    CATEGORY_ID, "C4.2 test category", null, true, CREATED_AT, UPDATED_AT
            ));
            assertThat(details.weeklySchedules()).containsExactly(
                    new WeeklySchedule(ENABLED_SCHEDULE_ID, PROFESSIONAL_ID,
                            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0),
                            30, true, CREATED_AT, UPDATED_AT),
                    new WeeklySchedule(DISABLED_SCHEDULE_ID, PROFESSIONAL_ID,
                            DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(13, 0),
                            20, false, CREATED_AT, UPDATED_AT)
            );
        } finally {
            transactions.executeWithoutResult(status -> {
                scheduleRepository.deleteAllByIdInBatch(List.of(
                        ENABLED_SCHEDULE_ID, DISABLED_SCHEDULE_ID, OTHER_SCHEDULE_ID
                ));
                professionalRepository.deleteAllByIdInBatch(List.of(
                        PROFESSIONAL_ID, OTHER_PROFESSIONAL_ID
                ));
                categoryRepository.deleteAllByIdInBatch(List.of(CATEGORY_ID));
            });
        }
    }

    @Test
    void detailsPortIsTransactionProxiedWithRepeatableReadReadOnlyMetadata() throws Exception {
        assertThat(AopUtils.isAopProxy(detailsPort)).isTrue();

        var method = LoadProfessionalDetailsPort.class.getMethod("findByProfessionalId", long.class);
        var attributes = new AnnotationTransactionAttributeSource()
                .getTransactionAttribute(method, AopUtils.getTargetClass(detailsPort));

        assertThat(attributes).isNotNull();
        assertThat(attributes.isReadOnly()).isTrue();
        assertThat(attributes.getIsolationLevel())
                .isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }
}
