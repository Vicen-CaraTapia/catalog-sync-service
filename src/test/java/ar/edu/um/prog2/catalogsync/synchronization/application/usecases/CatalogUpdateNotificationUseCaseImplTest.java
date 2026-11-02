package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogSyncMetadata;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.ApplyCatalogIncrementalVersionUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.RecoverCatalogFromSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisUnavailableException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.InvalidCatedraRedisDataException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.redis.exception.CatedraRedisAccessException;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadAppliedCatalogVersionPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogUpdateNotificationUseCaseImplTest {

    private final ReadAppliedCatalogVersionPort appliedVersionPort = mock(ReadAppliedCatalogVersionPort.class);
    private final ReadCatalogIncrementalChangesPort changesPort = mock(ReadCatalogIncrementalChangesPort.class);
    private final ApplyCatalogIncrementalVersionUseCase applyVersion =
            mock(ApplyCatalogIncrementalVersionUseCase.class);
    private final RecoverCatalogFromSnapshotUseCase recoverSnapshot = mock(RecoverCatalogFromSnapshotUseCase.class);
    private final ProcessedCatalogEventPort processedEvents = mock(ProcessedCatalogEventPort.class);
    private final CatalogUpdateNotificationUseCaseImpl useCase = new CatalogUpdateNotificationUseCaseImpl(
            appliedVersionPort, changesPort, applyVersion, recoverSnapshot, processedEvents
    );

    @Test
    void appliesOnePendingVersionBetweenMetadataChecks() {
        var version7 = changes(7);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(version7));

        useCase.onCatalogUpdated("event-7", 7);

        var order = inOrder(processedEvents, appliedVersionPort, changesPort, applyVersion);
        order.verify(processedEvents).findProcessedVersion("event-7");
        order.verify(appliedVersionPort).readAppliedVersion();
        order.verify(changesPort).readMetadata();
        order.verify(changesPort).readChanges(7);
        order.verify(applyVersion).apply(version7);
        order.verify(changesPort).readMetadata();
        order.verify(processedEvents).registerProcessed("event-7", 7);
        order.verifyNoMoreInteractions();
        verifyNoInteractions(recoverSnapshot);
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
    void recoversAnAlreadyAppliedNotificationWhenRedisHasAdvanced() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(7L);
        when(changesPort.readMetadata()).thenReturn(metadata(8));

        useCase.onCatalogUpdated("event-7", 7);

        verifyNoInteractions(applyVersion);
        verify(recoverSnapshot).recover(8);
        verify(processedEvents).registerProcessed("event-7", 7);
    }

    @Test
    void ignoresAnOlderNotificationWithoutReadingRedisOrReapplying() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(8L);

        useCase.onCatalogUpdated("event-7", 7);

        verifyNoInteractions(changesPort, applyVersion);
        verify(processedEvents).registerProcessed("event-7", 7);
    }

    @Test
    void rejectsAnUninitializedCatalogBeforeReadingRedis() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(null);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(changesPort, applyVersion);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void recoversWhenRedisHasAdvancedBeforeProcessing() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(8));

        useCase.onCatalogUpdated("event-7", 7);

        verifyNoInteractions(applyVersion);
        verify(changesPort).readMetadata();
        verify(recoverSnapshot).recover(8);
        verify(processedEvents).registerProcessed("event-7", 7);
    }

    @Test
    void recoversAtAMissingChangeSetAfterAnEarlierVersionWasApplied() {
        var version5 = changes(5);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(4L);
        when(changesPort.readMetadata()).thenReturn(metadata(6));
        when(changesPort.readChanges(5)).thenReturn(Optional.of(version5));
        when(changesPort.readChanges(6)).thenReturn(Optional.empty());

        useCase.onCatalogUpdated("event-6", 6);

        verify(applyVersion).apply(version5);
        verify(changesPort, times(1)).readMetadata();
        verify(recoverSnapshot).recover(6);
        verify(processedEvents).registerProcessed("event-6", 6);
    }

    @Test
    void rejectsAChangeSetForTheWrongVersionWithoutApplyingIt() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(8)));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyVersion);
        verifyNoInteractions(recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
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
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void recoversWhenRedisAdvancesAfterTheTargetWasApplied() {
        var version7 = changes(7);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7), metadata(8));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(version7));

        useCase.onCatalogUpdated("event-7", 7);

        verify(applyVersion).apply(version7);
        verify(changesPort, times(2)).readMetadata();
        var order = inOrder(applyVersion, recoverSnapshot, processedEvents);
        order.verify(applyVersion).apply(version7);
        order.verify(recoverSnapshot).recover(8);
        order.verify(processedEvents).registerProcessed("event-7", 7);
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

    @Test
    void recognizesProcessedEventBeforeAnySynchronizationWork() {
        when(processedEvents.findProcessedVersion("event-7")).thenReturn(OptionalLong.of(7));

        useCase.onCatalogUpdated("event-7", 7);

        verifyNoInteractions(appliedVersionPort, changesPort, applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void rejectsReusedEventIdWithAnotherVersionBeforeAnySynchronizationWork() {
        when(processedEvents.findProcessedVersion("event-7")).thenReturn(OptionalLong.of(8));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(ConflictingCatalogEventException.class);

        verifyNoInteractions(appliedVersionPort, changesPort, applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void recoversWhenLocalVersionIsBelowOldestAvailableVersion() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(3L);
        when(changesPort.readMetadata()).thenReturn(new CatalogSyncMetadata(7, 4, Instant.EPOCH));
        when(recoverSnapshot.recover(7)).thenReturn(8L);

        useCase.onCatalogUpdated("event-7", 7);

        verify(changesPort, never()).readChanges(4);
        verifyNoInteractions(applyVersion);
        var order = inOrder(recoverSnapshot, processedEvents);
        order.verify(recoverSnapshot).recover(7);
        order.verify(processedEvents).registerProcessed("event-7", 7);
    }

    @Test
    void acceptsLocalVersionEqualToOldestAvailableVersionForIncrementalProcessing() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(new CatalogSyncMetadata(7, 6, Instant.EPOCH));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(7)));

        useCase.onCatalogUpdated("event-7", 7);

        verify(applyVersion).apply(changes(7));
        verifyNoInteractions(recoverSnapshot);
    }

    @Test
    void rejectsMetadataWhoseOldestVersionExceedsCurrentVersion() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(new CatalogSyncMetadata(7, 8, Instant.EPOCH));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("metadata is inconsistent");

        verifyNoInteractions(applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void rejectsMissingMetadataWithoutGuessingThatHistoryIsMissing() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(null);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void rejectsRedisBehindNotificationWithoutRecovering() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(6));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("behind");

        verifyNoInteractions(applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @ParameterizedTest
    @MethodSource("redisFailures")
    void propagatesRedisMetadataFailureWithoutRecovering(RuntimeException failure) {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenThrow(failure);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verifyNoInteractions(applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @ParameterizedTest
    @MethodSource("redisFailures")
    void propagatesRedisChangesFailureWithoutClassifyingItAsMissingHistory(RuntimeException failure) {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7));
        when(changesPort.readChanges(7)).thenThrow(failure);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verifyNoInteractions(applyVersion, recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void neverRegistersEventAfterFailedRecovery() {
        var failure = new IllegalStateException("Controlled recovery failure");
        when(appliedVersionPort.readAppliedVersion()).thenReturn(3L);
        when(changesPort.readMetadata()).thenReturn(new CatalogSyncMetadata(7, 4, Instant.EPOCH));
        when(recoverSnapshot.recover(7)).thenThrow(failure);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void propagatesRegistrationFailureAfterSuccessfulProcessing() {
        var failure = new IllegalStateException("Controlled registration failure");
        when(appliedVersionPort.readAppliedVersion()).thenReturn(8L);
        doThrow(failure).when(processedEvents).registerProcessed("event-7", 7);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);
    }

    @Test
    void propagatesProcessedEventLookupFailureBeforeSynchronization() {
        var failure = new IllegalStateException("Controlled event lookup failure");
        when(processedEvents.findProcessedVersion("event-7")).thenThrow(failure);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verifyNoInteractions(appliedVersionPort, changesPort, applyVersion, recoverSnapshot);
    }

    @Test
    void rejectsRedisRegressionAfterCommittingPendingVersion() {
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7), metadata(6));
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(7)));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(IllegalStateException.class);

        verify(applyVersion).apply(changes(7));
        verifyNoInteractions(recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void finalMetadataFailureDoesNotRegisterAnEventWhoseVersionWasAlreadyApplied() {
        var failure = new CatedraRedisUnavailableException(null);
        when(appliedVersionPort.readAppliedVersion()).thenReturn(6L);
        when(changesPort.readMetadata()).thenReturn(metadata(7)).thenThrow(failure);
        when(changesPort.readChanges(7)).thenReturn(Optional.of(changes(7)));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verify(applyVersion).apply(changes(7));
        verifyNoInteractions(recoverSnapshot);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    private static Stream<RuntimeException> redisFailures() {
        return Stream.of(new CatedraRedisUnavailableException(null), new CatedraRedisAccessException(null),
                new InvalidCatedraRedisDataException());
    }

    private static CatalogSyncMetadata metadata(long version) {
        return new CatalogSyncMetadata(version, 1, Instant.EPOCH);
    }

    private static CatalogIncrementalChanges changes(long version) {
        return new CatalogIncrementalChanges(version, List.of(), List.of(), List.of());
    }
}
