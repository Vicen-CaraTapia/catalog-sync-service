package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JpaProfessionalDetailsAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-21T12:00:00Z");

    private ProfessionalJpaRepository professionals;
    private ProfessionalCategoryJpaRepository categories;
    private WeeklyScheduleJpaRepository schedules;
    private JpaProfessionalDetailsAdapter adapter;

    @BeforeEach
    void setUp() {
        professionals = mock(ProfessionalJpaRepository.class);
        categories = mock(ProfessionalCategoryJpaRepository.class);
        schedules = mock(WeeklyScheduleJpaRepository.class);
        adapter = new JpaProfessionalDetailsAdapter(professionals, categories, schedules);
    }

    @Test
    void loadsProfessionalCategoryAndAllSchedules() {
        var professional = new ProfessionalEntity(10L, 20L, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT);
        var category = new ProfessionalCategoryEntity(20L, "Salud", null, true, CREATED_AT, UPDATED_AT);
        var disabledSchedule = new WeeklyScheduleEntity(
                30L, 10L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0),
                30, false, CREATED_AT, UPDATED_AT
        );
        var enabledSchedule = new WeeklyScheduleEntity(
                31L, 10L, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(13, 0),
                30, true, CREATED_AT, UPDATED_AT
        );
        given(professionals.findById(10L)).willReturn(Optional.of(professional));
        given(categories.findById(20L)).willReturn(Optional.of(category));
        given(schedules.findByProfessionalIdOrderByIdAsc(10L))
                .willReturn(List.of(disabledSchedule, enabledSchedule));

        var details = adapter.findByProfessionalId(10L).orElseThrow();

        assertThat(details.professional().id()).isEqualTo(10L);
        assertThat(details.category().id()).isEqualTo(20L);
        assertThat(details.category().description()).isNull();
        assertThat(details.weeklySchedules()).extracting(schedule -> schedule.id())
                .containsExactly(30L, 31L);
        assertThat(details.weeklySchedules()).extracting(schedule -> schedule.enabled())
                .containsExactly(false, true);
        verify(schedules).findByProfessionalIdOrderByIdAsc(10L);
    }

    @Test
    void returnsEmptyOnlyWhenProfessionalDoesNotExist() {
        given(professionals.findById(10L)).willReturn(Optional.empty());

        assertThat(adapter.findByProfessionalId(10L)).isEmpty();

        verifyNoInteractions(categories, schedules);
    }

    @Test
    void reportsMissingCategoryAsLocalIntegrityFailure() {
        var professional = new ProfessionalEntity(10L, 20L, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT);
        given(professionals.findById(10L)).willReturn(Optional.of(professional));
        given(categories.findById(20L)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> adapter.findByProfessionalId(10L))
                .withMessage("Professional category is missing from the local catalog");

        verifyNoInteractions(schedules);
    }
}
