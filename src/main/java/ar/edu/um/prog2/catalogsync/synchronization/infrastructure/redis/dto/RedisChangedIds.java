package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto;

import java.util.List;

public record RedisChangedIds(
        List<Long> professionalCategories,
        List<Long> professionals,
        List<Long> weeklySchedules
) {
}
