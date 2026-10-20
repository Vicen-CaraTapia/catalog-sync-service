package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.client.CatedraCatalogSnapshotClient;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.CatalogSnapshotResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatedraCatalogSnapshotAdapterTest {

    @Mock
    private CatedraCatalogSnapshotClient client;

    @Test
    void returnsDomainSnapshotFromCatedraResponse() {
        Instant generatedAt = Instant.parse("2026-10-20T00:00:00Z");
        when(client.fetchSnapshot()).thenReturn(new CatalogSnapshotResponse(
                42L,
                generatedAt,
                List.of(),
                List.of(),
                List.of()
        ));

        CatalogSnapshot snapshot = new CatedraCatalogSnapshotAdapter(client).fetchSnapshot();

        assertThat(snapshot.version()).isEqualTo(42L);
        assertThat(snapshot.generatedAt()).isEqualTo(generatedAt);
        assertThat(snapshot.professionalCategories()).isEmpty();
        assertThat(snapshot.professionals()).isEmpty();
        assertThat(snapshot.weeklySchedules()).isEmpty();
    }
}
