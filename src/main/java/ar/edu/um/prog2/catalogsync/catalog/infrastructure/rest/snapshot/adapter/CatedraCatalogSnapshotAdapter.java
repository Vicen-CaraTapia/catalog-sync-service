package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.adapter;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.client.CatedraCatalogSnapshotClient;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.mapper.CatedraCatalogSnapshotMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class CatedraCatalogSnapshotAdapter implements FetchCatalogSnapshotPort {

    private final CatedraCatalogSnapshotClient client;

    public CatedraCatalogSnapshotAdapter(CatedraCatalogSnapshotClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public CatalogSnapshot fetchSnapshot() {
        return CatedraCatalogSnapshotMapper.toDomain(client.fetchSnapshot());
    }
}
