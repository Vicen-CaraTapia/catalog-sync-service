package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.application.service.CatalogVersionSynchronizationService;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ReconcileCatalogStateUseCaseImplTest {

    private final ReadAppliedCatalogVersionPort appliedVersionPort = mock(ReadAppliedCatalogVersionPort.class);
    private final CatalogVersionSynchronizationService synchronization = mock(CatalogVersionSynchronizationService.class);
    private final ReconcileCatalogStateUseCaseImpl useCase =
            new ReconcileCatalogStateUseCaseImpl(appliedVersionPort, synchronization);

    @Test
    void equalVersionsOnlyReadStateWithoutSynchronizing() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(9L);
        when(synchronization.readPublishedMetadata()).thenReturn(metadata(9));

        useCase.reconcile();

        var order = inOrder(appliedVersionPort, synchronization);
        order.verify(appliedVersionPort).readAppliedVersion();
        order.verify(synchronization).readPublishedMetadata();
        order.verifyNoMoreInteractions();
    }

    @Test
    void localBehindRedisDelegatesToTheSharedSynchronizationAlgorithm() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(synchronization.readPublishedMetadata()).thenReturn(metadata(9));

        useCase.reconcile();

        var order = inOrder(appliedVersionPort, synchronization);
        order.verify(appliedVersionPort).readAppliedVersion();
        order.verify(synchronization).readPublishedMetadata();
        order.verify(synchronization).synchronizeTo(9);
        order.verifyNoMoreInteractions();
    }

    @Test
    void localAheadOfRedisPreservesStateAndWarnsWithoutAttemptingDowngrade(CapturedOutput output) {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(9L);
        when(synchronization.readPublishedMetadata()).thenReturn(metadata(7));

        useCase.reconcile();

        var order = inOrder(appliedVersionPort, synchronization);
        order.verify(appliedVersionPort).readAppliedVersion();
        order.verify(synchronization).readPublishedMetadata();
        order.verifyNoMoreInteractions();
        assertThat(output.getOut()).contains(
                "Local catalog version is ahead of the observed Redis version; local state preserved.");
    }

    @Test
    void repeatedReconciliationReadsFreshStateAndDoesNotRepeatCompletedSynchronization() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L, 9L);
        when(synchronization.readPublishedMetadata()).thenReturn(metadata(9));

        useCase.reconcile();
        useCase.reconcile();

        verify(appliedVersionPort, times(2)).readAppliedVersion();
        verify(synchronization, times(2)).readPublishedMetadata();
        verify(synchronization).synchronizeTo(9);
    }

    @Test
    void uninitializedCatalogFailsBeforeReadingRedis() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(null);

        assertThatThrownBy(useCase::reconcile)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The local catalog has not been initialized");

        verifyNoInteractions(synchronization);
    }

    @Test
    void missingLocalStatePropagatesAsAnIntegrityFailure() {
        var failure = new IllegalStateException("catalog sync state is missing");
        when(appliedVersionPort.readAppliedVersion()).thenThrow(failure);

        assertThatThrownBy(useCase::reconcile).isSameAs(failure);

        verifyNoInteractions(synchronization);
    }

    @Test
    void metadataFailurePropagatesWithoutStartingSynchronization() {
        var failure = new IllegalStateException("Controlled metadata failure");
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(synchronization.readPublishedMetadata()).thenThrow(failure);

        assertThatThrownBy(useCase::reconcile).isSameAs(failure);

        var order = inOrder(synchronization);
        order.verify(synchronization).readPublishedMetadata();
        order.verifyNoMoreInteractions();
    }

    @Test
    void synchronizationFailurePropagatesWithoutAutomaticRetries() {
        var failure = new IllegalStateException("Controlled synchronization failure");
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(synchronization.readPublishedMetadata()).thenReturn(metadata(9));
        doThrow(failure).when(synchronization).synchronizeTo(9);

        assertThatThrownBy(useCase::reconcile).isSameAs(failure);

        var order = inOrder(synchronization);
        order.verify(synchronization).readPublishedMetadata();
        order.verify(synchronization).synchronizeTo(9);
        order.verifyNoMoreInteractions();
    }

    private static CatalogSyncMetadata metadata(long version) {
        return new CatalogSyncMetadata(version, 1, Instant.EPOCH);
    }
}
