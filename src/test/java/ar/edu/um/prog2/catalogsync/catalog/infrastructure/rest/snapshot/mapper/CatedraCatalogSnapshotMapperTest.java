package ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.CatalogSnapshotResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.ProfessionalCategoryResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.ProfessionalResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.dto.WeeklyScheduleResponse;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.rest.snapshot.exception.InvalidCatalogSnapshotException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatedraCatalogSnapshotMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Test
    void mapsCompleteSnapshotToDomainModel() {
        CatalogSnapshot snapshot = CatedraCatalogSnapshotMapper.toDomain(validResponse());

        assertThat(snapshot.version()).isEqualTo(7L);
        assertThat(snapshot.generatedAt()).isEqualTo(UPDATED_AT);
        assertThat(snapshot.professionalCategories()).singleElement().satisfies(category -> {
            assertThat(category.id()).isEqualTo(1L);
            assertThat(category.description()).isNull();
            assertThat(category.enabled()).isFalse();
        });
        assertThat(snapshot.professionals()).singleElement().satisfies(professional -> {
            assertThat(professional.id()).isEqualTo(15L);
            assertThat(professional.categoryId()).isEqualTo(1L);
            assertThat(professional.enabled()).isFalse();
        });
        assertThat(snapshot.weeklySchedules()).singleElement().satisfies(schedule -> {
            assertThat(schedule.id()).isEqualTo(21L);
            assertThat(schedule.professionalId()).isEqualTo(15L);
            assertThat(schedule.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(schedule.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(schedule.endTime()).isEqualTo(LocalTime.of(12, 0));
            assertThat(schedule.slotDurationMinutes()).isEqualTo(30);
            assertThat(schedule.enabled()).isFalse();
        });
    }

    @Test
    void translatesCrossCollectionContractViolations() {
        CatalogSnapshotResponse response = new CatalogSnapshotResponse(
                7L,
                UPDATED_AT,
                List.of(),
                List.of(new ProfessionalResponse(
                        15L,
                        1L,
                        "Ana",
                        "Gomez",
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )),
                List.of()
        );

        assertThatThrownBy(() -> CatedraCatalogSnapshotMapper.toDomain(response))
                .isInstanceOf(InvalidCatalogSnapshotException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private CatalogSnapshotResponse validResponse() {
        return new CatalogSnapshotResponse(
                7L,
                UPDATED_AT,
                List.of(new ProfessionalCategoryResponse(
                        1L,
                        "Clinical medicine",
                        null,
                        false,
                        CREATED_AT,
                        UPDATED_AT
                )),
                List.of(new ProfessionalResponse(
                        15L,
                        1L,
                        "Ana",
                        "Gomez",
                        false,
                        CREATED_AT,
                        UPDATED_AT
                )),
                List.of(new WeeklyScheduleResponse(
                        21L,
                        15L,
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        30,
                        false,
                        CREATED_AT,
                        UPDATED_AT
                ))
        );
    }
}
