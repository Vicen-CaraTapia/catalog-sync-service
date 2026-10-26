package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto;

public record RedisChangesResponse(
        Long version,
        RedisChangedIds changes
) {
}
