package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.decoder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisChangedIds;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisChangesResponse;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisProfessional;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisProfessionalCategory;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisWeeklySchedule;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class CatedraRedisCatalogDecoder {

    private final ObjectMapper objectMapper;

    public CatedraRedisCatalogDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public CatalogSyncMetadata decodeMetadata(
            String currentVersion,
            String oldestAvailableVersion,
            String publishedAt
    ) {
        return new CatalogSyncMetadata(
                decimalVersion(currentVersion),
                decimalVersion(oldestAvailableVersion),
                instant(publishedAt)
        );
    }

    public RedisChangesResponse decodeChanges(String json, long requestedVersion) {
        JsonNode root = object(json);
        long version = requiredLong(root, "version");
        if (version != requestedVersion) {
            throw invalid();
        }

        JsonNode changes = requiredObject(root, "changes");
        return new RedisChangesResponse(version, new RedisChangedIds(
                ids(changes, "professionalCategories"),
                ids(changes, "professionals"),
                ids(changes, "weeklySchedules")
        ));
    }

    public RedisProfessionalCategory decodeProfessionalCategory(String json, long requestedId) {
        JsonNode root = object(json);
        return new RedisProfessionalCategory(
                matchingId(root, requestedId),
                requiredText(root, "name"),
                optionalText(root, "description"),
                requiredBoolean(root, "enabled"),
                requiredInstant(root, "createdAt"),
                requiredInstant(root, "updatedAt")
        );
    }

    public RedisProfessional decodeProfessional(String json, long requestedId) {
        JsonNode root = object(json);
        return new RedisProfessional(
                matchingId(root, requestedId),
                requiredLong(root, "categoryId"),
                requiredText(root, "firstName"),
                requiredText(root, "lastName"),
                requiredBoolean(root, "enabled"),
                requiredInstant(root, "createdAt"),
                requiredInstant(root, "updatedAt")
        );
    }

    public RedisWeeklySchedule decodeWeeklySchedule(String json, long requestedId) {
        JsonNode root = object(json);
        int slotDurationMinutes = requiredInt(root, "slotDurationMinutes");
        if (slotDurationMinutes <= 0) {
            throw invalid();
        }

        return new RedisWeeklySchedule(
                matchingId(root, requestedId),
                requiredLong(root, "professionalId"),
                requiredDayOfWeek(root, "dayOfWeek"),
                requiredLocalTime(root, "startTime"),
                requiredLocalTime(root, "endTime"),
                slotDurationMinutes,
                requiredBoolean(root, "enabled"),
                requiredInstant(root, "createdAt"),
                requiredInstant(root, "updatedAt")
        );
    }

    private JsonNode object(String json) {
        if (json == null) {
            throw invalid();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw invalid();
            }
            return root;
        } catch (JacksonException exception) {
            throw invalid();
        }
    }

    private static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()) {
            throw invalid();
        }
        return value;
    }

    private static List<Long> ids(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isArray()) {
            throw invalid();
        }
        List<Long> result = new ArrayList<>();
        for (JsonNode id : value) {
            if (!id.isIntegralNumber() || !id.canConvertToLong()) {
                throw invalid();
            }
            result.add(id.longValue());
        }
        return List.copyOf(result);
    }

    private static long matchingId(JsonNode root, long requestedId) {
        long id = requiredLong(root, "id");
        if (id != requestedId) {
            throw invalid();
        }
        return id;
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

    private static boolean requiredBoolean(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isBoolean()) {
            throw invalid();
        }
        return value.booleanValue();
    }

    private static String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual()) {
            throw invalid();
        }
        return value.textValue();
    }

    private static String optionalText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalid();
        }
        return value.textValue();
    }

    private static Instant requiredInstant(JsonNode root, String field) {
        return instant(requiredText(root, field));
    }

    private static Instant instant(String value) {
        if (value == null) {
            throw invalid();
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid();
        }
    }

    private static DayOfWeek requiredDayOfWeek(JsonNode root, String field) {
        try {
            return DayOfWeek.valueOf(requiredText(root, field));
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private static LocalTime requiredLocalTime(JsonNode root, String field) {
        try {
            return LocalTime.parse(requiredText(root, field));
        } catch (DateTimeParseException exception) {
            throw invalid();
        }
    }

    private static long decimalVersion(String value) {
        if (value == null) {
            throw invalid();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    private static InvalidCatedraRedisDataException invalid() {
        return new InvalidCatedraRedisDataException();
    }
}
