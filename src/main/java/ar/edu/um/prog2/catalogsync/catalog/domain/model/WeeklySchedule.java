package ar.edu.um.prog2.catalogsync.catalog.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

public record WeeklySchedule(
        long id,
        long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public WeeklySchedule {
        Objects.requireNonNull(dayOfWeek, "dayOfWeek must be present");
        Objects.requireNonNull(startTime, "startTime must be present");
        Objects.requireNonNull(endTime, "endTime must be present");
        Objects.requireNonNull(createdAt, "createdAt must be present");
        Objects.requireNonNull(updatedAt, "updatedAt must be present");
        if (slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("slotDurationMinutes must be positive");
        }
    }
}
