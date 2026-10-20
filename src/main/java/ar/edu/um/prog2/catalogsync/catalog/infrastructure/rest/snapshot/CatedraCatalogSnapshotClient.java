package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot;

import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.CatedraRestClient;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest.exception.CatedraRestException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public final class CatedraCatalogSnapshotClient {

    private static final String SNAPSHOT_PATH = "/api/synchronization/snapshot";

    private final CatedraRestClient catedraRestClient;

    public CatedraCatalogSnapshotClient(CatedraRestClient catedraRestClient) {
        this.catedraRestClient = catedraRestClient;
    }

    public CatalogSnapshotResponse fetchSnapshot() {
        try {
            return catedraRestClient.get(SNAPSHOT_PATH, CatalogSnapshotResponse.class);
        }
        catch (CatedraRestException exception) {
            throw exception;
        }
        catch (RestClientException exception) {
            throw new InvalidCatalogSnapshotException(exception);
        }
    }
}
