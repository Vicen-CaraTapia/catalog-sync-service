package ar.edu.um.prog2.catalogsync.synchronization.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class CatalogSyncMetadataTest {

    @Test
    void retainsContractualVersionFieldsAndPublicationTime() {
        Instant publishedAt = Instant.parse("2026-09-10T13:44:59Z");

        var metadata = new CatalogSyncMetadata(7, 4, publishedAt);

        assertThat(metadata.currentVersion()).isEqualTo(7);
        assertThat(metadata.oldestAvailableVersion()).isEqualTo(4);
        assertThat(metadata.publishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void rejectsMissingPublicationTime() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CatalogSyncMetadata(7, 4, null));
    }
}
