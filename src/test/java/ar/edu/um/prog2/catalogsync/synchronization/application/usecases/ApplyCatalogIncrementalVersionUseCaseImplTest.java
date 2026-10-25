package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.CatalogIncrementalPersistencePort;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplyCatalogIncrementalVersionUseCaseImplTest {

    private final CatalogIncrementalPersistencePort persistence = mock(CatalogIncrementalPersistencePort.class);
    private final ApplyCatalogIncrementalVersionUseCaseImpl service =
            new ApplyCatalogIncrementalVersionUseCaseImpl(persistence);

    @Test
    void appliesOnlyTheNextVersion() {
        var changes = changes(7);
        when(persistence.readAppliedVersion()).thenReturn(6L);

        service.apply(changes);

        verify(persistence).applyAndAdvanceVersion(changes);
    }

    @Test
    void rejectsAnUninitializedCatalog() {
        var changes = changes(1);
        when(persistence.readAppliedVersion()).thenReturn(null);

        assertThatThrownBy(() -> service.apply(changes))
                .isInstanceOf(IllegalStateException.class);

        verify(persistence, never()).applyAndAdvanceVersion(changes);
    }

    @Test
    void rejectsSkippedOrRepeatedVersionsWithoutWriting() {
        when(persistence.readAppliedVersion()).thenReturn(6L);

        for (long version : List.of(6L, 8L)) {
            var changes = changes(version);
            assertThatThrownBy(() -> service.apply(changes))
                    .isInstanceOf(IllegalStateException.class);
            verify(persistence, never()).applyAndAdvanceVersion(changes);
        }
    }

    private static CatalogIncrementalChanges changes(long version) {
        return new CatalogIncrementalChanges(version, List.of(), List.of(), List.of());
    }
}
