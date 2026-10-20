package ar.edu.um.prog2.catalogsync.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.CatalogSnapshotPersistencePort;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplyCatalogSnapshotServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Mock
    private CatalogSnapshotPersistencePort persistencePort;

    private ApplyCatalogSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new ApplyCatalogSnapshotService(persistencePort);
    }

    @Test
    void appliesSnapshotThroughPersistencePort() {
        CatalogSnapshot snapshot = snapshot();

        service.apply(snapshot);

        verify(persistencePort).apply(snapshot);
    }

    @Test
    void propagatesPersistenceFailure() {
        CatalogSnapshot snapshot = snapshot();
        RuntimeException persistenceFailure = new RuntimeException("database failure");
        doThrow(persistenceFailure).when(persistencePort).apply(snapshot);

        assertThatThrownBy(() -> service.apply(snapshot))
                .isSameAs(persistenceFailure);
    }

    private CatalogSnapshot snapshot() {
        return new CatalogSnapshot(
                7L,
                UPDATED_AT,
                List.of(new ProfessionalCategory(
                        1L,
                        "Clinical medicine",
                        null,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )),
                List.of(new Professional(
                        15L,
                        1L,
                        "Ana",
                        "Gomez",
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )),
                List.of(new WeeklySchedule(
                        21L,
                        15L,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        30,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                ))
        );
    }
}
