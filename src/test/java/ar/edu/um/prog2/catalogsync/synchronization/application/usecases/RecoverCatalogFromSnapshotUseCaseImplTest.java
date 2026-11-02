package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.StaleCatalogSnapshotException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.InsufficientCatalogSnapshotVersionException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecoverCatalogFromSnapshotUseCaseImplTest {

    private final FetchCatalogSnapshotPort fetchSnapshot = mock(FetchCatalogSnapshotPort.class);
    private final ApplyCatalogSnapshotUseCase applySnapshot = mock(ApplyCatalogSnapshotUseCase.class);
    private final RecoverCatalogFromSnapshotUseCaseImpl useCase =
            new RecoverCatalogFromSnapshotUseCaseImpl(fetchSnapshot, applySnapshot);

    @ParameterizedTest
    @ValueSource(longs = {7, 8, Long.MAX_VALUE})
    void appliesSnapshotCoveringRequiredVersionAndReturnsItsActualVersion(long snapshotVersion) {
        var snapshot = snapshot(snapshotVersion);
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot);

        assertThat(useCase.recover(7)).isEqualTo(snapshotVersion);

        var order = inOrder(fetchSnapshot, applySnapshot);
        order.verify(fetchSnapshot).fetchSnapshot();
        order.verify(applySnapshot).apply(snapshot);
        order.verifyNoMoreInteractions();
    }

    @Test
    void rejectsAnOlderSnapshotBeforeApplyingIt() {
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot(6));

        assertThatThrownBy(() -> useCase.recover(7))
                .isInstanceOf(InsufficientCatalogSnapshotVersionException.class)
                .hasMessage("Catalog snapshot does not cover the minimum required version");

        verifyNoInteractions(applySnapshot);
    }

    @Test
    void propagatesFetchFailureWithoutApplyingOrRetrying() {
        var failure = new IllegalStateException("Controlled snapshot fetch failure");
        when(fetchSnapshot.fetchSnapshot()).thenThrow(failure);

        assertThatThrownBy(() -> useCase.recover(7)).isSameAs(failure);

        var order = inOrder(fetchSnapshot, applySnapshot);
        order.verify(fetchSnapshot).fetchSnapshot();
        order.verifyNoMoreInteractions();
    }

    @Test
    void propagatesApplicationFailureWithoutReturningASuccessfulVersion() {
        var snapshot = snapshot(8);
        var failure = new IllegalStateException("Controlled snapshot application failure");
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot);
        doThrow(failure).when(applySnapshot).apply(snapshot);

        assertThatThrownBy(() -> useCase.recover(7)).isSameAs(failure);

        var order = inOrder(fetchSnapshot, applySnapshot);
        order.verify(fetchSnapshot).fetchSnapshot();
        order.verify(applySnapshot).apply(snapshot);
        order.verifyNoMoreInteractions();
    }

    @Test
    void propagatesConcurrentVersionRegressionRejectionFromSnapshotApplication() {
        var snapshot = snapshot(8);
        var failure = new StaleCatalogSnapshotException();
        when(fetchSnapshot.fetchSnapshot()).thenReturn(snapshot);
        doThrow(failure).when(applySnapshot).apply(snapshot);

        assertThatThrownBy(() -> useCase.recover(7)).isSameAs(failure);
    }

    @Test
    void rejectsAnAbsentSnapshotBeforeApplication() {
        when(fetchSnapshot.fetchSnapshot()).thenReturn(null);

        assertThatThrownBy(() -> useCase.recover(7))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Catalog snapshot must be present");

        verifyNoInteractions(applySnapshot);
    }

    @Test
    void requiresBothExistingBoundaries() {
        assertThatThrownBy(() -> new RecoverCatalogFromSnapshotUseCaseImpl(null, applySnapshot))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecoverCatalogFromSnapshotUseCaseImpl(fetchSnapshot, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static CatalogSnapshot snapshot(long version) {
        return new CatalogSnapshot(version, Instant.EPOCH,
                List.of(new ProfessionalCategory(101L, "Disabled category", null, false,
                        Instant.EPOCH, Instant.EPOCH)), List.of(), List.of());
    }
}
