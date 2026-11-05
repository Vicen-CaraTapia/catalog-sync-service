package ar.edu.um.prog2.catalogsync.catalog.domain.ports.out;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;

public interface FetchCatalogSnapshotPort {

    CatalogSnapshot fetchSnapshot();
}
