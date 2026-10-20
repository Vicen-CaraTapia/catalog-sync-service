package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

public record WeeklyScheduleResponse(
        Long id,
        Long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public WeeklyScheduleResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(professionalId, "professionalId must be present");
        Objects.requireNonNull(dayOfWeek, "dayOfWeek must be present");
        Objects.requireNonNull(startTime, "startTime must be present");
        Objects.requireNonNull(endTime, "endTime must be present");
        Objects.requireNonNull(slotDurationMinutes, "slotDurationMinutes must be present");
        Objects.requireNonNull(enabled, "enabled must be present");
        Objects.requireNonNull(createdAt, "createdAt must be present");
        Objects.requireNonNull(updatedAt, "updatedAt must be present");
    }
}
