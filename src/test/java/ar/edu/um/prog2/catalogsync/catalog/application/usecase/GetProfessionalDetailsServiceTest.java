package ar.edu.um.prog2.catalogsync.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.LoadProfessionalDetailsPort;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetProfessionalDetailsServiceTest {

    @Test
    void returnsLocalDetailsIncludingDisabledSchedule() {
        LoadProfessionalDetailsPort port = mock(LoadProfessionalDetailsPort.class);
        GetProfessionalDetailsService service = new GetProfessionalDetailsService(port);
        Instant now = Instant.parse("2026-10-22T12:00:00Z");
        ProfessionalDetails expected = new ProfessionalDetails(
                new Professional(7L, 3L, "Ana", "García", true, now, now),
                new ProfessionalCategory(3L, "Cardiología", null, true, now, now),
                List.of(new WeeklySchedule(
                        11L, 7L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0),
                        30, false, now, now
                ))
        );
        when(port.findByProfessionalId(7L)).thenReturn(Optional.of(expected));

        ProfessionalDetails actual = service.getById(7L);

        assertThat(actual).isSameAs(expected);
        assertThat(actual.weeklySchedules().getFirst().enabled()).isFalse();
        verify(port).findByProfessionalId(7L);
    }

    @Test
    void throwsControlledExceptionWhenProfessionalDoesNotExist() {
        LoadProfessionalDetailsPort port = mock(LoadProfessionalDetailsPort.class);
        GetProfessionalDetailsService service = new GetProfessionalDetailsService(port);
        when(port.findByProfessionalId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ProfessionalNotFoundException.class);
        verify(port).findByProfessionalId(999L);
    }
}
