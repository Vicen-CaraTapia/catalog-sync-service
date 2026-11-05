package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.mapper;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.CatalogSnapshotResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.ProfessionalCategoryResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.ProfessionalResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.WeeklyScheduleResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.exception.InvalidCatalogSnapshotException;
import java.util.Objects;

public final class CatedraCatalogSnapshotMapper {

    private CatedraCatalogSnapshotMapper() {
    }

    public static CatalogSnapshot toDomain(CatalogSnapshotResponse response) {
        Objects.requireNonNull(response, "response must be present");

        try {
            return new CatalogSnapshot(
                    response.snapshotVersion(),
                    response.generatedAt(),
                    response.professionalCategories().stream()
                            .map(CatedraCatalogSnapshotMapper::toDomain)
                            .toList(),
                    response.professionals().stream()
                            .map(CatedraCatalogSnapshotMapper::toDomain)
                            .toList(),
                    response.weeklySchedules().stream()
                            .map(CatedraCatalogSnapshotMapper::toDomain)
                            .toList()
            );
        }
        catch (IllegalArgumentException exception) {
            throw new InvalidCatalogSnapshotException(exception);
        }
    }

    private static ProfessionalCategory toDomain(ProfessionalCategoryResponse response) {
        return new ProfessionalCategory(
                response.id(),
                response.name(),
                response.description(),
                response.enabled(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private static Professional toDomain(ProfessionalResponse response) {
        return new Professional(
                response.id(),
                response.categoryId(),
                response.firstName(),
                response.lastName(),
                response.enabled(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private static WeeklySchedule toDomain(WeeklyScheduleResponse response) {
        return new WeeklySchedule(
                response.id(),
                response.professionalId(),
                response.dayOfWeek(),
                response.startTime(),
                response.endTime(),
                response.slotDurationMinutes(),
                response.enabled(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
