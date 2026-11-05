package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.decoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.exception.InvalidCatalogUpdatedEventException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CatalogUpdatedEventDecoderTest {

	private final CatalogUpdatedEventDecoder decoder = new CatalogUpdatedEventDecoder(new ObjectMapper());

	@Test
	void decodesValidCatalogUpdatedEvent() {
		var event = decoder.decode(validJson());

		assertThat(event.eventId()).isEqualTo("catalog-event-7-4f3b");
		assertThat(event.eventType()).isEqualTo("CatalogUpdated");
		assertThat(event.newVersion()).isEqualTo(7L);
		assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
		assertThat(event.source()).isEqualTo("MANUAL");
		assertThat(event.schemaVersion()).isEqualTo(1);
	}

	@Test
	void rejectsMissingOrNullRequiredFields() {
		assertInvalid(validJson().replace("\"eventId\": \"catalog-event-7-4f3b\",", ""));
		assertInvalid(validJson().replace("\"eventId\": \"catalog-event-7-4f3b\"", "\"eventId\": null"));
		assertInvalid(validJson().replace("\"eventType\": \"CatalogUpdated\",", ""));
		assertInvalid(validJson().replace("\"newVersion\": 7,", ""));
		assertInvalid(validJson().replace("\"occurredAt\": \"2026-09-10T13:44:59Z\",", ""));
		assertInvalid(validJson().replace("\"source\": \"MANUAL\",", ""));
		assertInvalid(validJson().replace(",\n  \"schemaVersion\": 1", ""));
	}

	@Test
	void rejectsInvalidEventTypeAndUnsupportedSchemaVersion() {
		assertInvalid(validJson().replace("\"CatalogUpdated\"", "\"OtherEvent\""));
		assertInvalid(validJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"));
	}

	@Test
	void rejectsMalformedJsonAndIncompatibleFieldTypes() {
		assertInvalid("{not-json");
		assertInvalid(null);
		assertInvalid("[]");
		assertInvalid(validJson().replace("\"newVersion\": 7", "\"newVersion\": \"7\""));
		assertInvalid(validJson().replace("\"newVersion\": 7", "\"newVersion\": 7.5"));
		assertInvalid(validJson().replace("\"occurredAt\": \"2026-09-10T13:44:59Z\"", "\"occurredAt\": 7"));
		assertInvalid(validJson().replace("\"occurredAt\": \"2026-09-10T13:44:59Z\"", "\"occurredAt\": \"not-an-instant\""));
		assertInvalid(validJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": \"1\""));
	}

	@Test
	void ignoresAdditionalFieldsCompatibleWithVersionOne() {
		var event = decoder.decode(validJson().replace(
				"\"schemaVersion\": 1",
				"\"schemaVersion\": 1, \"futureOptionalField\": {\"value\": true}"
		));

		assertThat(event.newVersion()).isEqualTo(7L);
	}

	@Test
	void acceptsUnrecognizedNonBlankSourceValues() {
		var event = decoder.decode(validJson().replace("\"MANUAL\"", "\"OTHER_SOURCE\""));

		assertThat(event.source()).isEqualTo("OTHER_SOURCE");
	}

	@Test
	void rejectsBlankRequiredText() {
		assertInvalid(validJson().replace("\"catalog-event-7-4f3b\"", "\"  \""));
		assertInvalid(validJson().replace("\"MANUAL\"", "\"  \""));
	}

	private void assertInvalid(String json) {
		assertThatThrownBy(() -> decoder.decode(json))
				.isInstanceOf(InvalidCatalogUpdatedEventException.class)
				.hasMessage("Invalid CatalogUpdated event.");
	}

	private static String validJson() {
		return """
				{
				  "eventId": "catalog-event-7-4f3b",
				  "eventType": "CatalogUpdated",
				  "newVersion": 7,
				  "occurredAt": "2026-09-10T13:44:59Z",
				  "source": "MANUAL",
				  "schemaVersion": 1
				}
				""";
	}
}
