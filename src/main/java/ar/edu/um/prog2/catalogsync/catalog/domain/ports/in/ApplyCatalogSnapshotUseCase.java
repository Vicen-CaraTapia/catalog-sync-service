package ar.edu.um.prog2.catalogsync.catalog.domain.ports.in;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;

public interface ApplyCatalogSnapshotUseCase {

    void apply(CatalogSnapshot snapshot);
}
