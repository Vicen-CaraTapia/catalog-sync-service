package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto;

import java.time.Instant;

public record RedisProfessionalCategory(
        Long id,
        String name,
        String description,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
