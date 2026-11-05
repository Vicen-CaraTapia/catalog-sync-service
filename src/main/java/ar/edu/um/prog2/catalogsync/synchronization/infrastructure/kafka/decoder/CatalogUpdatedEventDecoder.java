package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.decoder;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.dto.CatalogUpdatedEvent;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class CatalogUpdatedEventDecoder {

	private final ObjectMapper objectMapper;

	public CatalogUpdatedEventDecoder(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
	}

	public CatalogUpdatedEvent decode(String json) {
		if (json == null) {
			throw invalid();
		}
		try {
			JsonNode root = objectMapper.readTree(json);
			if (root == null || !root.isObject()) {
				throw invalid();
			}

			String eventId = requiredText(root, "eventId");
			String eventType = requiredText(root, "eventType");
			long newVersion = requiredLong(root, "newVersion");
			Instant occurredAt = requiredInstant(root, "occurredAt");
			String source = requiredText(root, "source");
			int schemaVersion = requiredInt(root, "schemaVersion");

			if (!"CatalogUpdated".equals(eventType) || schemaVersion != 1) {
				throw invalid();
			}

			return new CatalogUpdatedEvent(
					eventId, eventType, newVersion, occurredAt, source, schemaVersion
			);
		} catch (JacksonException exception) {
			throw invalid();
		}
	}

	private static String requiredText(JsonNode root, String field) {
		JsonNode value = root.get(field);
		if (value == null || !value.isTextual() || value.textValue().isBlank()) {
			throw invalid();
		}
		return value.textValue();
	}

	private static long requiredLong(JsonNode root, String field) {
		JsonNode value = root.get(field);
		if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
			throw invalid();
		}
		return value.longValue();
	}

	private static int requiredInt(JsonNode root, String field) {
		JsonNode value = root.get(field);
		if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
			throw invalid();
		}
		return value.intValue();
	}

	private static Instant requiredInstant(JsonNode root, String field) {
		try {
			return Instant.parse(requiredText(root, field));
		} catch (DateTimeParseException exception) {
			throw invalid();
		}
	}

	private static InvalidCatalogUpdatedEventException invalid() {
		return new InvalidCatalogUpdatedEventException();
	}
}
