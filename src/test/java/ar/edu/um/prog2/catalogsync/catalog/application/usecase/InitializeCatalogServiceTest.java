package ar.edu.um.prog2.catalogsync.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogInitializationStatePort;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeCatalogServiceTest {

    @Mock
    private CatalogInitializationStatePort initializationStatePort;

    @Mock
    private FetchCatalogSnapshotPort fetchCatalogSnapshotPort;

    @Mock
    private ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    private InitializeCatalogService service;

    @BeforeEach
    void setUp() {
        service = new InitializeCatalogService(
                initializationStatePort,
                fetchCatalogSnapshotPort,
                applyCatalogSnapshotUseCase
        );
    }

    @Test
    void skipsSnapshotWhenCatalogIsAlreadyInitialized() {
        when(initializationStatePort.isInitialized()).thenReturn(true);

        service.initializeIfNeeded();

        verifyNoInteractions(fetchCatalogSnapshotPort, applyCatalogSnapshotUseCase);
    }

    @Test
    void fetchesAndAppliesSnapshotWhenCatalogIsUninitialized() {
        CatalogSnapshot snapshot = snapshot();
        when(fetchCatalogSnapshotPort.fetchSnapshot()).thenReturn(snapshot);

        service.initializeIfNeeded();

        verify(fetchCatalogSnapshotPort).fetchSnapshot();
        verify(applyCatalogSnapshotUseCase).apply(snapshot);
    }

    @Test
    void propagatesStateReadFailureWithoutFetchingSnapshot() {
        RuntimeException failure = new IllegalStateException("catalog sync state is missing");
        when(initializationStatePort.isInitialized()).thenThrow(failure);

        assertThatThrownBy(service::initializeIfNeeded).isSameAs(failure);

        verifyNoInteractions(fetchCatalogSnapshotPort, applyCatalogSnapshotUseCase);
    }

    @Test
    void propagatesSnapshotFetchFailureWithoutApplyingAnything() {
        RuntimeException failure = new RuntimeException("snapshot unavailable");
        when(fetchCatalogSnapshotPort.fetchSnapshot()).thenThrow(failure);

        assertThatThrownBy(service::initializeIfNeeded).isSameAs(failure);

        verifyNoInteractions(applyCatalogSnapshotUseCase);
    }

    @Test
    void propagatesSnapshotApplicationFailure() {
        CatalogSnapshot snapshot = snapshot();
        RuntimeException failure = new RuntimeException("database failure");
        when(fetchCatalogSnapshotPort.fetchSnapshot()).thenReturn(snapshot);
        doThrow(failure).when(applyCatalogSnapshotUseCase).apply(snapshot);

        assertThatThrownBy(service::initializeIfNeeded).isSameAs(failure);

        verify(fetchCatalogSnapshotPort).fetchSnapshot();
        verify(applyCatalogSnapshotUseCase).apply(snapshot);
    }

    private static CatalogSnapshot snapshot() {
        return new CatalogSnapshot(
                42L,
                Instant.parse("2026-10-20T00:00:00Z"),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
