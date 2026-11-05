package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.decoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CatedraRedisCatalogDecoderTest {

    private final CatedraRedisCatalogDecoder decoder = new CatedraRedisCatalogDecoder(new ObjectMapper());

    @Test
    void decodesMetadataHashFields() {
        var metadata = decoder.decodeMetadata("7", "4", "2026-09-10T13:44:59Z");

        assertThat(metadata.currentVersion()).isEqualTo(7);
        assertThat(metadata.oldestAvailableVersion()).isEqualTo(4);
        assertThat(metadata.publishedAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
    }

    @Test
    void rejectsMissingOrMalformedMetadataFields() {
        assertInvalid(() -> decoder.decodeMetadata(null, "4", "2026-09-10T13:44:59Z"));
        assertInvalid(() -> decoder.decodeMetadata("seven", "4", "2026-09-10T13:44:59Z"));
        assertInvalid(() -> decoder.decodeMetadata("7", "4.5", "2026-09-10T13:44:59Z"));
        assertInvalid(() -> decoder.decodeMetadata("7", "4", null));
        assertInvalid(() -> decoder.decodeMetadata("7", "4", "not-an-instant"));
    }

    @Test
    void decodesVersionSpecificAffectedIdsAndAllowsEmptyOrDuplicateLists() {
        var response = decoder.decodeChanges(changesJson(), 7);

        assertThat(response.version()).isEqualTo(7);
        assertThat(response.changes().professionalCategories()).isEmpty();
        assertThat(response.changes().professionals()).containsExactly(15L, 15L);
        assertThat(response.changes().weeklySchedules()).containsExactly(21L);

        var empty = decoder.decodeChanges("""
                {"version":7,"changes":{"professionalCategories":[],"professionals":[],"weeklySchedules":[]}}
                """, 7);
        assertThat(empty.changes().professionals()).isEmpty();
    }

    @Test
    void rejectsMissingMalformedOrMismatchedChangesets() {
        assertInvalid(() -> decoder.decodeChanges(null, 7));
        assertInvalid(() -> decoder.decodeChanges("not-json", 7));
        assertInvalid(() -> decoder.decodeChanges("[]", 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson(), 8));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"version\": 7", "\"version\": \"7\""), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"version\": 7", "\"version\": 7.5"), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"version\": 7,", ""), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"changes\": {", "\"changes\": null, \"other\": {"), 7));
    }

    @Test
    void rejectsMissingOrInvalidAffectedIdArrays() {
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"professionalCategories\": [],", ""), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"professionals\": [15, 15]", "\"professionals\": null"), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"professionals\": [15, 15]", "\"professionals\": [15, null]"), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"professionals\": [15, 15]", "\"professionals\": [\"15\"]"), 7));
        assertInvalid(() -> decoder.decodeChanges(changesJson().replace("\"weeklySchedules\": [21]", "\"weeklySchedules\": [21.5]"), 7));
    }

    @Test
    void decodesDisabledCategoryWithNullableDescription() {
        var category = decoder.decodeProfessionalCategory(categoryJson(), 1);

        assertThat(category.id()).isEqualTo(1);
        assertThat(category.description()).isNull();
        assertThat(category.enabled()).isFalse();
        assertThat(category.createdAt()).isEqualTo(Instant.parse("2026-07-01T12:00:00Z"));
    }

    @Test
    void rejectsInvalidCategoryFieldsAndHashFieldMismatch() {
        assertInvalid(() -> decoder.decodeProfessionalCategory(null, 1));
        assertInvalid(() -> decoder.decodeProfessionalCategory(categoryJson(), 2));
        assertInvalid(() -> decoder.decodeProfessionalCategory(categoryJson().replace("\"id\": 1", "\"id\": \"1\""), 1));
        assertInvalid(() -> decoder.decodeProfessionalCategory(categoryJson().replace("\"enabled\": false", "\"enabled\": \"false\""), 1));
        assertInvalid(() -> decoder.decodeProfessionalCategory(categoryJson().replace("\"description\": null", "\"description\": 7"), 1));
        assertInvalid(() -> decoder.decodeProfessionalCategory(categoryJson().replace("\"updatedAt\": \"2026-09-10T13:40:00Z\"", "\"updatedAt\": null"), 1));
    }

    @Test
    void decodesProfessionalAndRequiresItsContractFields() {
        var professional = decoder.decodeProfessional(professionalJson(), 15);

        assertThat(professional.id()).isEqualTo(15);
        assertThat(professional.categoryId()).isEqualTo(1);
        assertThat(professional.firstName()).isEqualTo("Ana");
        assertThat(professional.enabled()).isTrue();

        assertInvalid(() -> decoder.decodeProfessional(professionalJson(), 16));
        assertInvalid(() -> decoder.decodeProfessional(professionalJson().replace("\"categoryId\": 1", "\"categoryId\": null"), 15));
        assertInvalid(() -> decoder.decodeProfessional(professionalJson().replace("\"firstName\": \"Ana\"", "\"firstName\": 7"), 15));
    }

    @Test
    void decodesWeeklyScheduleWithBothContractualTimeFormats() {
        var schedule = decoder.decodeWeeklySchedule(scheduleJson(), 21);

        assertThat(schedule.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedule.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(schedule.slotDurationMinutes()).isEqualTo(30);
        assertThat(schedule.enabled()).isFalse();
    }

    @Test
    void rejectsInvalidWeeklyScheduleFields() {
        assertInvalid(() -> decoder.decodeWeeklySchedule(scheduleJson(), 22));
        assertInvalid(() -> decoder.decodeWeeklySchedule(scheduleJson().replace("\"dayOfWeek\": \"MONDAY\"", "\"dayOfWeek\": \"FUNDAY\""), 21));
        assertInvalid(() -> decoder.decodeWeeklySchedule(scheduleJson().replace("\"startTime\": \"09:00\"", "\"startTime\": \"bad\""), 21));
        assertInvalid(() -> decoder.decodeWeeklySchedule(scheduleJson().replace("\"endTime\": \"12:00:00\"", "\"endTime\": null"), 21));
        assertInvalid(() -> decoder.decodeWeeklySchedule(scheduleJson().replace("\"slotDurationMinutes\": 30", "\"slotDurationMinutes\": 0"), 21));
        assertInvalid(() -> decoder.decodeWeeklySchedule(scheduleJson().replace("\"slotDurationMinutes\": 30", "\"slotDurationMinutes\": 30.5"), 21));
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(InvalidCatedraRedisDataException.class)
                .hasMessage("Invalid Catedra Redis synchronization data.");
    }

    private static String changesJson() {
        return """
                {
                  "version": 7,
                  "changes": {
                    "professionalCategories": [],
                    "professionals": [15, 15],
                    "weeklySchedules": [21],
                    "futureField": true
                  }
                }
                """;
    }

    private static String categoryJson() {
        return """
                {
                  "id": 1,
                  "name": "Clinica medica",
                  "description": null,
                  "enabled": false,
                  "createdAt": "2026-07-01T12:00:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z",
                  "futureField": true
                }
                """;
    }

    private static String professionalJson() {
        return """
                {
                  "id": 15,
                  "categoryId": 1,
                  "firstName": "Ana",
                  "lastName": "Gomez",
                  "enabled": true,
                  "createdAt": "2026-07-01T12:00:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
                """;
    }

    private static String scheduleJson() {
        return """
                {
                  "id": 21,
                  "professionalId": 15,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00",
                  "endTime": "12:00:00",
                  "slotDurationMinutes": 30,
                  "enabled": false,
                  "createdAt": "2026-07-01T12:00:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
                """;
    }
}
