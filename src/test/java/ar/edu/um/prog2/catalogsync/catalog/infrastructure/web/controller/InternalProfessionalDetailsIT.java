package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/** Real security filters, signed JWTs, controller, use case and PostgreSQL adapter. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalProfessionalDetailsIT {
    private static final String PATH = "/internal/api/professionals/-71002";

    @DynamicPropertySource
    static void internalJwtTestKey(DynamicPropertyRegistry registry) {
        InternalJwtTestKeys.register(registry);
    }

    @Autowired private MockMvc mvc;
    @Autowired private DataSource dataSource;
    @MockitoBean private FetchCatalogSnapshotPort snapshot;
    @MockitoBean private ReadCatalogIncrementalChangesPort redis;
    private JdbcTemplate jdbc;
    private boolean ownsFixtures;
    private Long appliedVersion;

    @BeforeEach
    void prepareLocalCatalog() {
        jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).containsIgnoringCase("test");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM professional_categories WHERE id = -71001", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM professionals WHERE id = -71002", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_schedules WHERE id IN (-71003, -71004)", Integer.class)).isZero();
        appliedVersion = jdbc.queryForObject("SELECT applied_version FROM catalog_sync_state WHERE state_id = 1", Long.class);
        ownsFixtures = true;
        jdbc.update("INSERT INTO professional_categories VALUES (-71001, 'Salud', 'Private description', false, now(), now())");
        jdbc.update("INSERT INTO professionals VALUES (-71002, -71001, 'Ana', 'Gomez', false, now(), now())");
        jdbc.update("INSERT INTO weekly_schedules VALUES (-71003, -71002, 'MONDAY', '09:00', '12:00', 30, true, now(), now())");
        jdbc.update("INSERT INTO weekly_schedules VALUES (-71004, -71002, 'TUESDAY', '10:00', '13:00', 20, false, now(), now())");
    }

    @AfterEach
    void removeOnlyOwnedFixtures() {
        if (ownsFixtures) {
            try {
                assertThat(jdbc.queryForObject("SELECT applied_version FROM catalog_sync_state WHERE state_id = 1", Long.class))
                        .isEqualTo(appliedVersion);
                verifyNoInteractions(snapshot, redis);
            } finally {
                jdbc.update("DELETE FROM weekly_schedules WHERE id IN (-71003, -71004)");
                jdbc.update("DELETE FROM professionals WHERE id = -71002");
                jdbc.update("DELETE FROM professional_categories WHERE id = -71001");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void authorizedClientReadsTheExactContractFromPostgres(boolean enabled) throws Exception {
        jdbc.update("UPDATE professionals SET enabled = ? WHERE id = -71002", enabled);
        mvc.perform(get(PATH).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {"id":-71002,"firstName":"Ana","lastName":"Gomez","enabled":%s,
                     "category":{"id":-71001,"name":"Salud","enabled":false},
                     "weeklySchedules":[
                       {"id":-71004,"dayOfWeek":"TUESDAY","startTime":"10:00:00","endTime":"13:00:00","slotDurationMinutes":20,"enabled":false},
                       {"id":-71003,"dayOfWeek":"MONDAY","startTime":"09:00:00","endTime":"12:00:00","slotDurationMinutes":30,"enabled":true}]}
                    """.formatted(enabled), JsonCompareMode.STRICT))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void authorizedClientReceivesAnEmptyArrayWhenThereAreNoSchedules() throws Exception {
        jdbc.update("DELETE FROM weekly_schedules WHERE id IN (-71003, -71004)");
        mvc.perform(get(PATH).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weeklySchedules").isArray())
                .andExpect(jsonPath("$.weeklySchedules").isEmpty());
    }

    @Test
    void authorizedUnknownIdReturnsTheDocumentedProblem() throws Exception {
        mvc.perform(get("/internal/api/professionals/9223372036854775807").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                    {"status":404,"title":"Not Found","detail":"Professional not found.",
                     "instance":"/internal/api/professionals/9223372036854775807"}
                    """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void authorizedInvalidIdReturns400InsteadOfAnErrorDispatch403(String id) throws Exception {
        mvc.perform(get("/internal/api/professionals/" + id).header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Professional ID must be a valid 64-bit integer."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "expired"})
    void rejectsUnauthenticatedRequests(String scenario) throws Exception {
        var request = get(PATH);
        if (scenario.equals("invalid")) {
            request.header("Authorization", "Bearer invalid-contract-test-token");
        } else if (scenario.equals("expired")) {
            var claims = InternalJwtTestKeys.claims();
            claims.put("exp", Instant.now().minusSeconds(120).getEpochSecond());
            request.header("Authorization", "Bearer " + InternalJwtTestKeys.sign(claims));
        }
        mvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                    {"status":401,"title":"Unauthorized","detail":"Internal service authentication is required."}
                    """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "catalog:read-extra"})
    void rejectsAuthenticatedClientsWithoutTheExactPermission(String scope) throws Exception {
        var claims = InternalJwtTestKeys.claims();
        claims.put("scope", scope);
        mvc.perform(get(PATH).header("Authorization", "Bearer " + InternalJwtTestKeys.sign(claims)))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"insufficient_scope\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                    {"status":403,"title":"Forbidden","detail":"Access to this internal operation is denied."}
                    """, JsonCompareMode.STRICT));
    }

    private static String bearer() throws Exception {
        return "Bearer " + InternalJwtTestKeys.sign(InternalJwtTestKeys.claims());
    }
}
