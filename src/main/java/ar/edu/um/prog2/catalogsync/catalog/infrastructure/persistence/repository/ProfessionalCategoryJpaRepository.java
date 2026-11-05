package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalCategoryJpaRepository extends JpaRepository<ProfessionalCategoryEntity, Long> {
}
