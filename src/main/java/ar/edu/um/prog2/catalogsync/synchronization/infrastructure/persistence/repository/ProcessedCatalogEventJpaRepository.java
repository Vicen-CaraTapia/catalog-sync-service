package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.repository;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.entity.ProcessedCatalogEventEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedCatalogEventJpaRepository extends JpaRepository<ProcessedCatalogEventEntity, String> {

    @Query("select event.newVersion from ProcessedCatalogEventEntity event where event.eventId = :eventId")
    Optional<Long> findProcessedVersion(@Param("eventId") String eventId);

    @Modifying
    @Query(value = """
            INSERT INTO processed_catalog_events (event_id, new_version)
            VALUES (:eventId, :newVersion)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId, @Param("newVersion") long newVersion);
}
