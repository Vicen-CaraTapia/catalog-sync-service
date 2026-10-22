package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyScheduleJpaRepository extends JpaRepository<WeeklyScheduleEntity, Long> {
}
