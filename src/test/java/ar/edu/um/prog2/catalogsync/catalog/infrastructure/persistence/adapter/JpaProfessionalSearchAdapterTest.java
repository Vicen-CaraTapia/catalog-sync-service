package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaProfessionalSearchAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-21T12:00:00Z");

    private ProfessionalJpaRepository repository;
    private JpaProfessionalSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(ProfessionalJpaRepository.class);
        adapter = new JpaProfessionalSearchAdapter(repository);
    }

    @Test
    void mapsPageAndRequestsDeterministicSort() {
        ProfessionalEntity entity = entity(13L, 7L, "Ana", "Gomez", false);
        PageRequest pageable = PageRequest.of(1, 2, Sort.by("lastName", "firstName", "id"));
        given(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(entity), pageable, 5));

        ProfessionalSearchResult result = adapter.search(
                new ProfessionalSearchCriteria(null, null, null, null, 1, 2)
        );

        assertThat(result.content()).containsExactly(
                new Professional(13L, 7L, "Ana", "Gomez", false, CREATED_AT, UPDATED_AT)
        );
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);

        ArgumentCaptor<PageRequest> pageableCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("lastName").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("firstName").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsNullCriteria() {
        assertThatNullPointerException().isThrownBy(() -> adapter.search(null));
    }

    @Test
    void buildsNameCategoryAndEnabledPredicatesWithLiteralLikeEscaping() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                "A%_!", 7L, false, null, 0, 20
        );
        Root<ProfessionalEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> field = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> nameExpression = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(root.get(anyString())).thenReturn(field);
        when(builder.lower(any(Expression.class))).thenReturn(nameExpression);
        when(builder.like(eq(nameExpression), anyString(), any(char.class))).thenReturn(predicate);
        when(builder.or(predicate, predicate)).thenReturn(predicate);
        when(builder.equal(eq(field), any())).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(combined);

        Specification<ProfessionalEntity> specification = specificationFor(criteria);
        Predicate result = specification.toPredicate(root, query, builder);

        assertThat(result).isSameAs(combined);
        ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder, org.mockito.Mockito.times(2))
                .like(eq(nameExpression), patternCaptor.capture(), eq('!'));
        assertThat(patternCaptor.getAllValues()).containsExactly("%a!%!_!!%", "%a!%!_!!%");
        verify(root).get("firstName");
        verify(root).get("lastName");
        verify(root).get("categoryId");
        verify(root).get("enabled");
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(builder, org.mockito.Mockito.times(2)).equal(eq(field), valueCaptor.capture());
        assertThat(valueCaptor.getAllValues()).containsExactly(7L, false);
        verify(builder).and(any(Predicate[].class));
    }

    @Test
    void returnsUnfilteredPredicateWhenNoFiltersArePresent() {
        Root<ProfessionalEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        when(builder.and()).thenReturn(predicate);

        assertThat(specificationFor(new ProfessionalSearchCriteria(null, null, null, null, 0, 20))
                .toPredicate(root, query, builder)).isSameAs(predicate);

        verify(builder).and();
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsCorrelatedExistsForEnabledScheduleOnRequestedWeekday() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                null, null, null, LocalDate.of(2026, 10, 26), 0, 20
        );
        Root<ProfessionalEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Subquery<Long> subquery = mock(Subquery.class);
        Root<WeeklyScheduleEntity> schedule = mock(Root.class);
        Path<Object> field = mock(Path.class);
        Predicate exists = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(WeeklyScheduleEntity.class)).thenReturn(schedule);
        when(root.get("id")).thenReturn(field);
        when(schedule.get(anyString())).thenReturn(field);
        when(subquery.select(any())).thenReturn(subquery);
        when(builder.exists(subquery)).thenReturn(exists);
        when(builder.and(any(Predicate[].class))).thenReturn(combined);

        Predicate result = specificationFor(criteria).toPredicate(root, query, builder);

        assertThat(result).isSameAs(combined);
        verify(schedule).get("professionalId");
        verify(schedule).get("enabled");
        verify(schedule).get("dayOfWeek");
        verify(builder).equal(field, field);
        verify(builder).isTrue(any());
        verify(builder).equal(field, DayOfWeek.MONDAY);
        verify(builder).exists(subquery);
        verify(builder).and(exists);
    }

    @SuppressWarnings("unchecked")
    private Specification<ProfessionalEntity> specificationFor(ProfessionalSearchCriteria criteria) {
        given(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        adapter.search(criteria);
        ArgumentCaptor<Specification<ProfessionalEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), any(PageRequest.class));
        return captor.getValue();
    }

    private static ProfessionalEntity entity(
            long id,
            long categoryId,
            String firstName,
            String lastName,
            boolean enabled
    ) {
        return new ProfessionalEntity(id, categoryId, firstName, lastName, enabled, CREATED_AT, UPDATED_AT);
    }
}
