package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.client.CatedraRedisClient;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.decoder.CatedraRedisCatalogDecoder;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisProfessional;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisProfessionalCategory;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.dto.RedisWeeklySchedule;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class CatedraRedisCatalogChangesAdapter implements ReadCatalogIncrementalChangesPort {

    private final CatedraRedisClient client;
    private final CatedraRedisCatalogDecoder decoder;

    public CatedraRedisCatalogChangesAdapter(CatedraRedisClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.decoder = new CatedraRedisCatalogDecoder(objectMapper);
    }

    @Override
    public CatalogSyncMetadata readMetadata() {
        var fields = client.readMetadataFields();
        return decoder.decodeMetadata(
                fields.get("currentVersion"),
                fields.get("oldestAvailableVersion"),
                fields.get("publishedAt")
        );
    }

    @Override
    public Optional<CatalogIncrementalChanges> readChanges(long version) {
        var json = client.readChangesJson(version);
        if (json.isEmpty()) {
            return Optional.empty();
        }

        var affectedIds = decoder.decodeChanges(json.get(), version).changes();
        return Optional.of(new CatalogIncrementalChanges(
                version,
                categories(affectedIds.professionalCategories()),
                professionals(affectedIds.professionals()),
                schedules(affectedIds.weeklySchedules())
        ));
    }

    private List<ProfessionalCategory> categories(List<Long> ids) {
        var json = client.readProfessionalCategoryJson(ids);
        return IntStream.range(0, ids.size())
                .mapToObj(index -> toDomain(decoder.decodeProfessionalCategory(json.get(index), ids.get(index))))
                .toList();
    }

    private List<Professional> professionals(List<Long> ids) {
        var json = client.readProfessionalJson(ids);
        return IntStream.range(0, ids.size())
                .mapToObj(index -> toDomain(decoder.decodeProfessional(json.get(index), ids.get(index))))
                .toList();
    }

    private List<WeeklySchedule> schedules(List<Long> ids) {
        var json = client.readWeeklyScheduleJson(ids);
        return IntStream.range(0, ids.size())
                .mapToObj(index -> toDomain(decoder.decodeWeeklySchedule(json.get(index), ids.get(index))))
                .toList();
    }

    private static ProfessionalCategory toDomain(RedisProfessionalCategory category) {
        return new ProfessionalCategory(
                category.id(), category.name(), category.description(), category.enabled(),
                category.createdAt(), category.updatedAt()
        );
    }

    private static Professional toDomain(RedisProfessional professional) {
        return new Professional(
                professional.id(), professional.categoryId(), professional.firstName(),
                professional.lastName(), professional.enabled(), professional.createdAt(),
                professional.updatedAt()
        );
    }

    private static WeeklySchedule toDomain(RedisWeeklySchedule schedule) {
        return new WeeklySchedule(
                schedule.id(), schedule.professionalId(), schedule.dayOfWeek(),
                schedule.startTime(), schedule.endTime(), schedule.slotDurationMinutes(),
                schedule.enabled(), schedule.createdAt(), schedule.updatedAt()
        );
    }
}
