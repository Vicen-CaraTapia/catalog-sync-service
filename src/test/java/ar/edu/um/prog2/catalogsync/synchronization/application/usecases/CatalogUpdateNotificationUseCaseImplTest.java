package ar.edu.um.prog2.catalogsync.synchronization.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.application.service.CatalogVersionSynchronizationService;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ProcessedCatalogEventPort;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CatalogUpdateNotificationUseCaseImplTest {

    private final CatalogVersionSynchronizationService synchronization = mock(CatalogVersionSynchronizationService.class);
    private final ProcessedCatalogEventPort processedEvents = mock(ProcessedCatalogEventPort.class);
    private final CatalogUpdateNotificationUseCaseImpl useCase =
            new CatalogUpdateNotificationUseCaseImpl(synchronization, processedEvents);

    @Test
    void synchronizesUnknownEventBeforeDurableRegistration() {
        useCase.onCatalogUpdated("event-7", 7);

        var order = inOrder(processedEvents, synchronization);
        order.verify(processedEvents).findProcessedVersion("event-7");
        order.verify(synchronization).synchronizeTo(7);
        order.verify(processedEvents).registerProcessed("event-7", 7);
        order.verifyNoMoreInteractions();
    }

    @Test
    void recognizesProcessedEventWithoutSynchronizingOrRegisteringAgain() {
        when(processedEvents.findProcessedVersion("event-7")).thenReturn(OptionalLong.of(7));

        useCase.onCatalogUpdated("event-7", 7);

        verifyNoInteractions(synchronization);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void rejectsReusedEventIdWithAnotherVersionBeforeSynchronization() {
        when(processedEvents.findProcessedVersion("event-7")).thenReturn(OptionalLong.of(8));

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7))
                .isInstanceOf(ConflictingCatalogEventException.class);

        verifyNoInteractions(synchronization);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void neverRegistersAnEventAfterSynchronizationFailure() {
        var failure = new IllegalStateException("Controlled synchronization failure");
        doThrow(failure).when(synchronization).synchronizeTo(7);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void propagatesProcessedEventLookupFailureWithoutSynchronizing() {
        var failure = new IllegalStateException("Controlled event lookup failure");
        when(processedEvents.findProcessedVersion("event-7")).thenThrow(failure);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        verifyNoInteractions(synchronization);
        verify(processedEvents, never()).registerProcessed("event-7", 7);
    }

    @Test
    void propagatesRegistrationFailureAfterSynchronizationWithoutReportingSuccess() {
        var failure = new IllegalStateException("Controlled registration failure");
        doThrow(failure).when(processedEvents).registerProcessed("event-7", 7);

        assertThatThrownBy(() -> useCase.onCatalogUpdated("event-7", 7)).isSameAs(failure);

        var order = inOrder(processedEvents, synchronization);
        order.verify(processedEvents).findProcessedVersion("event-7");
        order.verify(synchronization).synchronizeTo(7);
        order.verify(processedEvents).registerProcessed("event-7", 7);
        order.verifyNoMoreInteractions();
    }

    @Test
    void rejectsMissingEventIdBeforeAccessingAnyDependency() {
        assertThatThrownBy(() -> useCase.onCatalogUpdated(null, 7)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(processedEvents, synchronization);
    }

    @Test
    void requiresSynchronizationAndDurableEventPersistence() {
        assertThatThrownBy(() -> new CatalogUpdateNotificationUseCaseImpl(null, processedEvents))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CatalogUpdateNotificationUseCaseImpl(synchronization, null))
                .isInstanceOf(NullPointerException.class);
    }
}
