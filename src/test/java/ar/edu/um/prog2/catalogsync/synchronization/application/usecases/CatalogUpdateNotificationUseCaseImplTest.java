package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogUpdateNotificationUseCaseImplTest {

    private final ReadAppliedCatalogVersionPort appliedVersionPort = mock(ReadAppliedCatalogVersionPort.class);
    private final ReadCatalogIncrementalChangesPort changesPort = mock(ReadCatalogIncrementalChangesPort.class);
    private final ApplyCatalogIncrementalVersionUseCase applyVersion =
            mock(ApplyCatalogIncrementalVersionUseCase.class);
    private final CatalogUpdateNotificationUseCaseImpl useCase = new CatalogUpdateNotificationUseCaseImpl(
            appliedVersionPort, changesPort, applyVersion
    );

    @Test
    void appliesOnePendingVersionBetweenMetadataChecks() {
        var version7 = changes(7);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(version7));

        useCase.onCatalogUpdated("event-7", 7);

        var order = inOrder(appliedVersionPort, changesPort, applyVersion);
        order.verify(appliedVersionPort).readAppliedVersion();
        order.verify(changesPort).readMetadata();
        order.verify(changesPort).readChanges(7);
        order.verify(applyVersion).apply(version7);
        order.verify(changesPort).readMetadata();
        order.verifyNoMoreInteractions();
    }

    @Test
    void appliesEveryPendingVersionInOrder() {
        var version5 = changes(5);
        var version6 = changes(6);
        var version7 = changes(7);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(4L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(5)).thenReturn(Optional.of(version5));
        when(changesPort.readChanges(6)).thenReturn(Optional.of(version6));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(version7));

        useCase.onCatalogUpdated("event-7", 7);

        var order = inOrder(changesPort, applyVersion);
        order.verify(changesPort).readMetadata();
        order.verify(changesPort).readChanges(5);
        order.verify(applyVersion).apply(version5);
        order.verify(changesPort).readChanges(6);
        order.verify(applyVersion).apply(version6);
        order.verify(changesPort).readChanges(7);
        order.verify(applyVersion).apply(version7);
        order.verify(changesPort).readMetadata();
        order.verifyNoMoreInteractions();
    }

    @Test
    void treatsAnAlreadyAppliedVersionAsNoOpOnlyWhenRedisStillMatches() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));

        useCase.onCatalogUpdated("event-7", 7);

        verify(changesPort).readMetadata();
        verifyNoInteractions(applyVersion);
    }

    @Test
    void rejectsAnAlreadyAppliedVersionWhenRedisHasAdvanced() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(changesPort.readMetadata()).thenReturn(metadata(8));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyVersion);
    }

    @Test
    void ignoresAnOlderNotificationWithoutReadingRedisOrReapplying() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(8L);

        useCase.onCatalogUpdated("event-7", 7);

        verifyNoInteractions(changesPort, applyVersion);
    }

    @Test
    void rejectsAnUninitializedCatalogBeforeReadingRedis() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(null);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(changesPort, applyVersion);
    }

    @Test
    void rejectsRedisVersionMismatchBeforeReadingChanges() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(8));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyVersion);
        verify(changesPort).readMetadata();
    }

    @Test
    void stopsAtAMissingChangeSetAfterAnEarlierVersionWasApplied() {
        var version5 = changes(5);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(4L);
        when(changesPort.readMetadata()).thenReturn(metadata(6));
        when(changesPort.readChanges(5)).thenReturn(Optional.of(version5));
        when(changesPort.readChanges(6)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-6", 6))
                .isInstanceOf(IllegalStateException.class);

        verify(applyVersion).apply(version5);
        verify(changesPort, times(1)).readMetadata();
    }

    @Test
    void rejectsAChangeSetForTheWrongVersionWithoutApplyingIt() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(8)));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyVersion);
    }

    @Test
    void propagatesApplicationFailureWithoutReadingFinalMetadata() {
        var version7 = changes(7);
        var failure = new IllegalStateException("Test persistence failure");
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(version7));
        doThrow(failure).when(applyVersion).apply(version7);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verify(changesPort, times(1)).readMetadata();
    }

    @Test
    void rejectsRedisAdvanceAfterTheTargetWasApplied() {
        var version7 = changes(7);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7), metadata(8));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(version7));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verify(applyVersion).apply(version7);
        verify(changesPort, times(2)).readMetadata();
    }

    @Test
    void handlesTheMaximumLongVersionWithoutOverflow() {
        var maximumVersion = changes(Long.MAX_VALUE);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(Long.MAX_VALUE - 1);
        when(changesPort.readMetadata()).thenReturn(metadata(Long.MAX_VALUE));
        when(changesPort.readChanges(Long.MAX_VALUE)).thenReturn(Optional.of(maximumVersion));

        useCase.onCatalogUpdated("last-event", Long.MAX_VALUE);

        verify(applyVersion).apply(maximumVersion);
        verify(changesPort, times(2)).readMetadata();
    }

    private static CatalogSyncMetadata metadata(long version) {
        return new CatalogSyncMetadata(version, 1, Instant.EPOCH);
    }

    private static CatalogIncrementalChanges changes(long version) {
        return new CatalogIncrementalChanges(version, List.of(), List.of(), List.of());
    }
}
