package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.synchronization.application.exception.ConflictingCatalogEventException;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.repository.ProcessedCatalogEventJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

class JpaProcessedCatalogEventAdapterTest {

    private final ProcessedCatalogEventJpaRepository repository = mock(ProcessedCatalogEventJpaRepository.class);
    private final JpaProcessedCatalogEventAdapter adapter = new JpaProcessedCatalogEventAdapter(repository);

    @Test
    void returnsEmptyForAnUnregisteredEvent() {
        when(repository.findProcessedVersion("event-7")).thenReturn(Optional.empty());

        assertThat(adapter.findProcessedVersion("event-7")).isEmpty();
    }

    @Test
    void returnsTheStoredVersionWithoutNarrowing() {
        when(repository.findProcessedVersion("event-last")).thenReturn(Optional.of(Long.MAX_VALUE));

        assertThat(adapter.findProcessedVersion("event-last")).hasValue(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void acceptsMatchingVersionForBothExistingAndNewRegistrations(int insertedRows) {
        when(repository.insertIfAbsent("event-7", 7)).thenReturn(insertedRows);
        when(repository.findProcessedVersion("event-7")).thenReturn(Optional.of(7L));

        adapter.registerProcessed("event-7", 7);

        var order = inOrder(repository);
        order.verify(repository).insertIfAbsent("event-7", 7);
        order.verify(repository).findProcessedVersion("event-7");
        order.verifyNoMoreInteractions();
    }

    @Test
    void rejectsAConflictingVersionWithASanitizedMessage() {
        String id = "external-event-not-for-error-message";
        when(repository.findProcessedVersion(id)).thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> adapter.registerProcessed(id, 8))
                .isInstanceOf(ConflictingCatalogEventException.class)
                .hasMessage("Catalog event ID is already associated with a different version")
                .hasNoCause();
    }

    @Test
    void failsExplicitlyWhenRegistrationCannotBeReadBack() {
        when(repository.findProcessedVersion("event-7")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.registerProcessed("event-7", 7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Processed catalog event registration is missing");
    }

    @Test
    void propagatesInsertFailureWithoutAttemptingReadBack() {
        var failure = new DataAccessResourceFailureException("Controlled insert failure");
        when(repository.insertIfAbsent("event-7", 7)).thenThrow(failure);

        assertThatThrownBy(() -> adapter.registerProcessed("event-7", 7)).isSameAs(failure);

        var order = inOrder(repository);
        order.verify(repository).insertIfAbsent("event-7", 7);
        order.verifyNoMoreInteractions();
    }

    @Test
    void propagatesReadBackFailureInsteadOfReportingRegistrationSuccess() {
        var failure = new DataAccessResourceFailureException("Controlled read failure");
        when(repository.findProcessedVersion("event-7")).thenThrow(failure);

        assertThatThrownBy(() -> adapter.registerProcessed("event-7", 7)).isSameAs(failure);
    }

    @Test
    void propagatesLookupFailureInsteadOfReportingAnUnregisteredEvent() {
        var failure = new DataAccessResourceFailureException("Controlled lookup failure");
        when(repository.findProcessedVersion("event-7")).thenThrow(failure);

        assertThatThrownBy(() -> adapter.findProcessedVersion("event-7")).isSameAs(failure);
    }

    @Test
    void rejectsNullEventIdsBeforeCallingPersistence() {
        assertThatThrownBy(() -> adapter.findProcessedVersion(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> adapter.registerProcessed(null, 7)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void requiresARepository() {
        assertThatThrownBy(() -> new JpaProcessedCatalogEventAdapter(null))
                .isInstanceOf(NullPointerException.class);
    }
}
