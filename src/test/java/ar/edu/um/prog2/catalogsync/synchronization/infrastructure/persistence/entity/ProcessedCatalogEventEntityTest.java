package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProcessedCatalogEventEntityTest {

    @Test
    void exposesValuesHydratedThroughJpaFieldAccess() {
        var entity = new ProcessedCatalogEventEntity();
        // JPA uses the protected constructor and populates fields directly, without setters.
        ReflectionTestUtils.setField(entity, "eventId", "catalog-event-7");
        ReflectionTestUtils.setField(entity, "newVersion", Long.MAX_VALUE);

        assertThat(entity.getEventId()).isEqualTo("catalog-event-7");
        assertThat(entity.getNewVersion()).isEqualTo(Long.MAX_VALUE);
    }
}
