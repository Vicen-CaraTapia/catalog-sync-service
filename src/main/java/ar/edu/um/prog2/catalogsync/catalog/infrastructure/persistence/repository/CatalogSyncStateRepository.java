package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogSyncStateRepository extends JpaRepository<CatalogSyncStateEntity, Short> {
}
