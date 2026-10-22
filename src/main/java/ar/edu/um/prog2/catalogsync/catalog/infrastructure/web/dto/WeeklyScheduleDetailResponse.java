package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

public record WeeklyScheduleDetailResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean enabled
) {
    public WeeklyScheduleDetailResponse {
        Objects.requireNonNull(id, "id must be present");
        Objects.requireNonNull(dayOfWeek, "dayOfWeek must be present");
        Objects.requireNonNull(startTime, "startTime must be present");
        Objects.requireNonNull(endTime, "endTime must be present");
    }
}
