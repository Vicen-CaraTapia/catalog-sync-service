package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaCatalogAppliedVersionReadAdapter implements ReadAppliedCatalogVersionPort {

    private final CatalogSyncStateRepository stateRepository;

    public JpaCatalogAppliedVersionReadAdapter(CatalogSyncStateRepository stateRepository) {
        this.stateRepository = Objects.requireNonNull(stateRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Long readAppliedVersion() {
        return stateRepository.findById(CatalogSyncStateEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("catalog sync state is missing"))
                .getAppliedVersion();
    }
}
