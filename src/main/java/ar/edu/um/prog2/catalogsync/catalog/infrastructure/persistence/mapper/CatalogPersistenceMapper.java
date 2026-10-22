package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.mapper;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;

public final class CatalogPersistenceMapper {

    private CatalogPersistenceMapper() {
    }

    public static ProfessionalCategoryEntity toEntity(ProfessionalCategory category) {
        return new ProfessionalCategoryEntity(
                category.id(),
                category.name(),
                category.description(),
                category.enabled(),
                category.createdAt(),
                category.updatedAt()
        );
    }

    public static ProfessionalEntity toEntity(Professional professional) {
        return new ProfessionalEntity(
                professional.id(),
                professional.categoryId(),
                professional.firstName(),
                professional.lastName(),
                professional.enabled(),
                professional.createdAt(),
                professional.updatedAt()
        );
    }

    public static Professional toDomain(ProfessionalEntity entity) {
        return new Professional(
                entity.getId(),
                entity.getCategoryId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static WeeklyScheduleEntity toEntity(WeeklySchedule weeklySchedule) {
        return new WeeklyScheduleEntity(
                weeklySchedule.id(),
                weeklySchedule.professionalId(),
                weeklySchedule.dayOfWeek(),
                weeklySchedule.startTime(),
                weeklySchedule.endTime(),
                weeklySchedule.slotDurationMinutes(),
                weeklySchedule.enabled(),
                weeklySchedule.createdAt(),
                weeklySchedule.updatedAt()
        );
    }
}
