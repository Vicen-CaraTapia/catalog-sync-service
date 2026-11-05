package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.WeeklySchedule;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import ar.edu.um.prog2.catalogsync.synchronization.domain.model.CatalogIncrementalChanges;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class JpaCatalogIncrementalPersistenceAdapterTest {

    private static final Instant CREATED = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-09-02T00:00:00Z");

    @Mock
    private CatalogSyncStateRepository stateRepository;
    @Mock
    private ProfessionalCategoryJpaRepository categoryRepository;
    @Mock
    private ProfessionalJpaRepository professionalRepository;
    @Mock
    private WeeklyScheduleJpaRepository scheduleRepository;

    @Captor
    private ArgumentCaptor<Iterable<ProfessionalCategoryEntity>> categoriesCaptor;
    @Captor
    private ArgumentCaptor<Iterable<ProfessionalEntity>> professionalsCaptor;
    @Captor
    private ArgumentCaptor<Iterable<WeeklyScheduleEntity>> schedulesCaptor;

    private JpaCatalogIncrementalPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCatalogIncrementalPersistenceAdapter(
                stateRepository, categoryRepository, professionalRepository, scheduleRepository);
    }

    @Test
    void readsTheAppliedVersionFromTheSingletonState() {
        when(stateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.of(new CatalogSyncStateEntity(6L)));

        assertThat(adapter.readAppliedVersion()).isEqualTo(6L);
        verifyNoInteractions(categoryRepository, professionalRepository, scheduleRepository);
    }

    @Test
    void reportsMissingSingletonStateAsAnIntegrityFailure() {
        when(stateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(adapter::readAppliedVersion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("catalog sync state is missing");
    }

    @Test
    void writesAffectedEntitiesInForeignKeyOrderBeforeAdvancingVersion() {
        CatalogSyncStateEntity state = new CatalogSyncStateEntity(6L);
        when(stateRepository.findByStateId(CatalogSyncStateEntity.SINGLETON_ID))
                .thenReturn(Optional.of(state));

        adapter.applyAndAdvanceVersion(changes());

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        verify(professionalRepository).saveAll(professionalsCaptor.capture());
        verify(scheduleRepository).saveAll(schedulesCaptor.capture());
        ProfessionalCategoryEntity category = categoriesCaptor.getValue().iterator().next();
        ProfessionalEntity professional = professionalsCaptor.getValue().iterator().next();
        WeeklyScheduleEntity schedule = schedulesCaptor.getValue().iterator().next();
        assertThat(category.getId()).isEqualTo(101L);
        assertThat(category.isEnabled()).isFalse();
        assertThat(professional.getCategoryId()).isEqualTo(category.getId());
        assertThat(professional.isEnabled()).isFalse();
        assertThat(schedule.getProfessionalId()).isEqualTo(professional.getId());
        assertThat(schedule.isEnabled()).isFalse();

        var order = inOrder(categoryRepository, professionalRepository, scheduleRepository, stateRepository);
        order.verify(categoryRepository).saveAll(any());
        order.verify(categoryRepository).flush();
        order.verify(professionalRepository).saveAll(any());
        order.verify(professionalRepository).flush();
        order.verify(scheduleRepository).saveAll(any());
        order.verify(scheduleRepository).flush();
        order.verify(stateRepository).findByStateId(CatalogSyncStateEntity.SINGLETON_ID);
        order.verify(stateRepository).flush();
        assertThat(state.getAppliedVersion()).isEqualTo(7L);
    }

    @Test
    void doesNotAdvanceVersionWhenACollectionFlushFails() {
        doThrow(new DataIntegrityViolationException("test constraint failure"))
                .when(scheduleRepository).flush();

        assertThatThrownBy(() -> adapter.applyAndAdvanceVersion(changes()))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(stateRepository, never()).findByStateId(any());
        verify(stateRepository, never()).flush();
    }

    private static CatalogIncrementalChanges changes() {
        return new CatalogIncrementalChanges(7L,
                List.of(new ProfessionalCategory(101L, "Category", null, false, CREATED, UPDATED)),
                List.of(new Professional(201L, 101L, "Ana", "Test", false, CREATED, UPDATED)),
                List.of(new WeeklySchedule(301L, 201L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0), 30, false, CREATED, UPDATED)));
    }
}
