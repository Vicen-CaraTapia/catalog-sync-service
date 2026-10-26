package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto;

import java.time.Instant;

public record RedisProfessional(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
