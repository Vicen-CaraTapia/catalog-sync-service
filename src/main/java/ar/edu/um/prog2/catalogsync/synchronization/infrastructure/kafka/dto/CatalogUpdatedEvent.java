package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.dto;

import java.time.Instant;

public record CatalogUpdatedEvent(
		String eventId,
		String eventType,
		Long newVersion,
		Instant occurredAt,
		String source,
		Integer schemaVersion
) {
}
