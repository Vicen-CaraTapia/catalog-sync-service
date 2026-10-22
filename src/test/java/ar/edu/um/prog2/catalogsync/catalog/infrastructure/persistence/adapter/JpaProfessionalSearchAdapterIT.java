package ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchCriteria;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.search.ProfessionalSearchResult;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uses real PostgreSQL queries. Fixture inserts are rolled back after each test;
 * existing catalog records and the applied version are not modified.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class JpaProfessionalSearchAdapterIT {

    private static final long CATEGORY_ID = -41_001L;
    private static final long OTHER_CATEGORY_ID = -41_002L;
    private static final long FIRST_ID = -41_101L;
    private static final long SECOND_ID = -41_102L;
    private static final long THIRD_ID = -41_103L;
    private static final long FOURTH_ID = -41_104L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Autowired
    private SearchProfessionalsUseCase searchProfessionals;

    @Autowired
    private ProfessionalCategoryJpaRepository categoryRepository;

    @Autowired
    private ProfessionalJpaRepository professionalRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void insertIsolatedCategories() {
        assertThat(categoryRepository.existsById(CATEGORY_ID)).isFalse();
        assertThat(categoryRepository.existsById(OTHER_CATEGORY_ID)).isFalse();
        categoryRepository.saveAllAndFlush(List.of(
                category(CATEGORY_ID),
                category(OTHER_CATEGORY_ID)
        ));
    }

    @Test
    void searchesWithoutFiltersIncludingBothEnabledStatesAndCategories() {
        long previousCount = professionalRepository.count();
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true),
                professional(SECOND_ID, OTHER_CATEGORY_ID, "Luis", "Perez", false)
        );

        ProfessionalSearchResult result = search(null, null, null, 0, 20);

        assertThat(result.totalElements()).isEqualTo(previousCount + 2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.content()).hasSize((int) Math.min(previousCount + 2, 20));
    }

    @Test
    void filtersByExactCategoryAndMapsAllFieldsIncludingDisabledProfessionals() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true),
                professional(SECOND_ID, CATEGORY_ID, "Luis", "Perez", false),
                professional(THIRD_ID, OTHER_CATEGORY_ID, "Marta", "Diaz", true)
        );

        ProfessionalSearchResult result = search(null, CATEGORY_ID, null, 0, 20);

        assertThat(result.content()).containsExactly(
                new Professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT),
                new Professional(SECOND_ID, CATEGORY_ID, "Luis", "Perez", false, CREATED_AT, UPDATED_AT)
        );
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void matchesTrimmedPartialNameIgnoringCaseInFirstNameOrLastName() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true),
                professional(SECOND_ID, CATEGORY_ID, "Luis", "Anaya", false),
                professional(THIRD_ID, CATEGORY_ID, "Marta", "Perez", true)
        );

        ProfessionalSearchResult result = search("  nA  ", CATEGORY_ID, null, 0, 20);

        assertThat(result.content()).extracting(Professional::id)
                .containsExactly(SECOND_ID, FIRST_ID);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void searchesAcrossCategoriesWhenCategoryIsOmitted() {
        String uniqueName = "C41Search" + UUID.randomUUID();
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, uniqueName, "Gomez", true),
                professional(SECOND_ID, OTHER_CATEGORY_ID, "Luis", uniqueName, false)
        );

        ProfessionalSearchResult result = search(uniqueName, null, null, 0, 20);

        assertThat(result.content()).extracting(Professional::id)
                .containsExactlyInAnyOrder(FIRST_ID, SECOND_ID);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void filtersByExactEnabledState() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true),
                professional(SECOND_ID, CATEGORY_ID, "Luis", "Perez", false)
        );

        assertThat(search(null, CATEGORY_ID, true, 0, 20).content())
                .extracting(Professional::id).containsExactly(FIRST_ID);
        assertThat(search(null, CATEGORY_ID, false, 0, 20).content())
                .extracting(Professional::id).containsExactly(SECOND_ID);
    }

    @Test
    void combinesNameCategoryAndEnabledWithAnd() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true),
                professional(SECOND_ID, CATEGORY_ID, "Ana", "Perez", false),
                professional(THIRD_ID, CATEGORY_ID, "Luis", "Diaz", true),
                professional(FOURTH_ID, OTHER_CATEGORY_ID, "Luis", "Anaya", true)
        );

        ProfessionalSearchResult result = search("ana", CATEGORY_ID, true, 0, 20);

        assertThat(result.content()).extracting(Professional::id).containsExactly(FIRST_ID);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void treatsLikeWildcardsAndEscapeCharacterAsLiterals() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana%Literal", "Gomez", true),
                professional(SECOND_ID, CATEGORY_ID, "Ana_Literal", "Gomez", true),
                professional(THIRD_ID, CATEGORY_ID, "Ana!Literal", "Gomez", true),
                professional(FOURTH_ID, CATEGORY_ID, "AnaXLiteral", "Gomez", true)
        );

        assertThat(search("aNA%", CATEGORY_ID, null, 0, 20).content())
                .extracting(Professional::id).containsExactly(FIRST_ID);
        assertThat(search("ANA_", CATEGORY_ID, null, 0, 20).content())
                .extracting(Professional::id).containsExactly(SECOND_ID);
        assertThat(search("ana!", CATEGORY_ID, null, 0, 20).content())
                .extracting(Professional::id).containsExactly(THIRD_ID);
    }

    @Test
    void treatsBlankNameAsNoNameFilter() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true),
                professional(SECOND_ID, CATEGORY_ID, "Luis", "Perez", false)
        );

        ProfessionalSearchResult result = search(" \t\n ", CATEGORY_ID, null, 0, 20);

        assertThat(result).isEqualTo(search(null, CATEGORY_ID, null, 0, 20));
        assertThat(result.content()).hasSize(2);
    }

    @Test
    void paginatesInLastNameFirstNameAndIdAscendingOrder() {
        insertProfessionals(
                professional(FIRST_ID, CATEGORY_ID, "Luis", "Alfa", true),
                professional(SECOND_ID, CATEGORY_ID, "Ana", "Alfa", false),
                professional(THIRD_ID, CATEGORY_ID, "Ana", "Alfa", true),
                professional(FOURTH_ID, CATEGORY_ID, "Alicia", "Beta", true)
        );

        ProfessionalSearchResult firstPage = search(null, CATEGORY_ID, null, 0, 2);
        ProfessionalSearchResult secondPage = search(null, CATEGORY_ID, null, 1, 2);

        assertThat(firstPage.content()).extracting(Professional::id)
                .containsExactly(THIRD_ID, SECOND_ID);
        assertThat(secondPage.content()).extracting(Professional::id)
                .containsExactly(FIRST_ID, FOURTH_ID);
        assertThat(firstPage.totalElements()).isEqualTo(4);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(2);
        assertThat(search(null, CATEGORY_ID, null, 0, 2)).isEqualTo(firstPage);
    }

    @Test
    void returnsEmptyPageWhenNoProfessionalsMatch() {
        insertProfessionals(professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true));

        ProfessionalSearchResult result = search("NoMatch", CATEGORY_ID, null, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void returnsEmptyPageBeyondLastPageWithoutLosingTotals() {
        insertProfessionals(professional(FIRST_ID, CATEGORY_ID, "Ana", "Gomez", true));

        ProfessionalSearchResult result = search(null, CATEGORY_ID, null, 5, 2);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(5);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    private ProfessionalSearchResult search(String name, Long categoryId, Boolean enabled, int page, int size) {
        return searchProfessionals.search(new ProfessionalSearchCriteria(name, categoryId, enabled, page, size));
    }

    private void insertProfessionals(ProfessionalEntity... professionals) {
        for (ProfessionalEntity professional : professionals) {
            assertThat(professionalRepository.existsById(professional.getId())).isFalse();
        }
        professionalRepository.saveAllAndFlush(List.of(professionals));
        entityManager.clear();
    }

    private static ProfessionalCategoryEntity category(long id) {
        return new ProfessionalCategoryEntity(id, "C4.1 test category", null, true, CREATED_AT, UPDATED_AT);
    }

    private static ProfessionalEntity professional(
            long id, long categoryId, String firstName, String lastName, boolean enabled
    ) {
        return new ProfessionalEntity(id, categoryId, firstName, lastName, enabled, CREATED_AT, UPDATED_AT);
    }
}
