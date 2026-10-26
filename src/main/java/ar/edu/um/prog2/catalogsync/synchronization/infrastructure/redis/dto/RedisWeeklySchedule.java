package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record RedisWeeklySchedule(
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
}
