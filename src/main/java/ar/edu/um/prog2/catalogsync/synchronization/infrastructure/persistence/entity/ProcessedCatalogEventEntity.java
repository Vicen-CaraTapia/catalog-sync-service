package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_catalog_events")
public class ProcessedCatalogEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, columnDefinition = "TEXT")
    private String eventId;

    @Column(name = "new_version", nullable = false)
    private long newVersion;

    protected ProcessedCatalogEventEntity() {
    }

    public String getEventId() {
        return eventId;
    }

    public long getNewVersion() {
        return newVersion;
    }
}
