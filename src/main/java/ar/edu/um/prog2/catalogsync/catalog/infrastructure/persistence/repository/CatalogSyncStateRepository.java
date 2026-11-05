package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CatalogSyncStateRepository extends JpaRepository<CatalogSyncStateEntity, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CatalogSyncStateEntity> findByStateId(Short stateId);
}
