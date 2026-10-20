package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_sync_state")
public class CatalogSyncStateEntity {

    public static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "state_id")
    private Short stateId;

    @Column(name = "applied_version")
    private Long appliedVersion;

    protected CatalogSyncStateEntity() {
    }

    public CatalogSyncStateEntity(Long appliedVersion) {
        this.stateId = SINGLETON_ID;
        this.appliedVersion = appliedVersion;
    }

    public Short getStateId() {
        return stateId;
    }

    public Long getAppliedVersion() {
        return appliedVersion;
    }

    public void updateAppliedVersion(long appliedVersion) {
        this.appliedVersion = appliedVersion;
    }
}
