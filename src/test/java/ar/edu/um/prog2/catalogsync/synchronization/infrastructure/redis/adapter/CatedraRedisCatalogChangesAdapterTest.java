package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client.CatedraRedisClient;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CatedraRedisCatalogChangesAdapterTest {

    @Mock
    private CatedraRedisClient client;

    @Test
    void readsMetadataAsAnInternalModel() {
        when(client.readMetadataFields()).thenReturn(Map.of(
                "currentVersion", "7",
                "oldestAvailableVersion", "4",
                "publishedAt", "2026-09-10T13:44:59Z"
        ));

        var metadata = adapter().readMetadata();

        assertThat(metadata.currentVersion()).isEqualTo(7);
        assertThat(metadata.oldestAvailableVersion()).isEqualTo(4);
        assertThat(metadata.publishedAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
    }

    @Test
    void returnsEmptyOnlyForAnAbsentChangesetKey() {
        when(client.readChangesJson(7)).thenReturn(Optional.empty());

        assertThat(adapter().readChanges(7)).isEmpty();

        verify(client).readChangesJson(7);
        verifyNoMoreInteractions(client);
    }

    @Test
    void resolvesAffectedIdsToCurrentHashStateAndMapsToDomain() {
        when(client.readChangesJson(7)).thenReturn(Optional.of("""
                {"version":7,"changes":{"professionalCategories":[1],"professionals":[15],"weeklySchedules":[21]}}
                """));
        when(client.readProfessionalCategoryJson(List.of(1L))).thenReturn(List.of("""
                {"id":1,"name":"Clinica medica","description":null,"enabled":false,
                 "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                """));
        when(client.readProfessionalJson(List.of(15L))).thenReturn(List.of("""
                {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true,
                 "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                """));
        when(client.readWeeklyScheduleJson(List.of(21L))).thenReturn(List.of("""
                {"id":21,"professionalId":15,"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00:00",
                 "slotDurationMinutes":30,"enabled":false,
                 "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                """));

        var changes = adapter().readChanges(7).orElseThrow();

        assertThat(changes.requestedChangeVersion()).isEqualTo(7);
        assertThat(changes.currentProfessionalCategories()).singleElement().satisfies(category -> {
            assertThat(category.id()).isEqualTo(1);
            assertThat(category.description()).isNull();
            assertThat(category.enabled()).isFalse();
        });
        assertThat(changes.currentProfessionals()).singleElement().satisfies(professional -> {
            assertThat(professional.id()).isEqualTo(15);
            assertThat(professional.categoryId()).isEqualTo(1);
            assertThat(professional.enabled()).isTrue();
        });
        assertThat(changes.currentWeeklySchedules()).singleElement().satisfies(schedule -> {
            assertThat(schedule.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(schedule.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(schedule.enabled()).isFalse();
        });
    }

    @Test
    void doesNotTurnInvalidChangesetIntoAbsence() {
        when(client.readChangesJson(7)).thenReturn(Optional.of("{not-json"));

        assertThatThrownBy(() -> adapter().readChanges(7))
                .isInstanceOf(InvalidCatedraRedisDataException.class);
    }

    @Test
    void propagatesMissingAffectedEntityState() {
        when(client.readChangesJson(7)).thenReturn(Optional.of("""
                {"version":7,"changes":{"professionalCategories":[],"professionals":[15],"weeklySchedules":[]}}
                """));
        when(client.readProfessionalJson(List.of(15L)))
                .thenThrow(new InvalidCatedraRedisDataException());

        assertThatThrownBy(() -> adapter().readChanges(7))
                .isInstanceOf(InvalidCatedraRedisDataException.class);
    }

    private CatedraRedisCatalogChangesAdapter adapter() {
        return new CatedraRedisCatalogChangesAdapter(client, new ObjectMapper());
    }
}
