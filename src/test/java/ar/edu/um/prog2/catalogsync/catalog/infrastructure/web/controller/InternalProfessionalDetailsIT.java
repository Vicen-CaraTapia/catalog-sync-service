package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.out.FetchCatalogSnapshotPort;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.exception.ProfessionalDetailsExceptionHandler;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.out.ReadCatalogIncrementalChangesPort;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Real controller, use case and PostgreSQL adapter; isolated MVC dispatch does not modify security. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class InternalProfessionalDetailsIT {
    @org.springframework.test.context.DynamicPropertySource
    static void internalJwtTestKey(org.springframework.test.context.DynamicPropertyRegistry registry) {
        ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys.register(registry);
    }

    @Autowired private InternalProfessionalDetailsController controller;
    @Autowired private ProfessionalDetailsExceptionHandler advice;
    @Autowired private DataSource dataSource;
    @MockitoBean private FetchCatalogSnapshotPort snapshot;
    @MockitoBean private ReadCatalogIncrementalChangesPort redis;

    @Test
    void readsOnlyLocalCatalogAndReturnsTheExactInternalContract() throws Exception {
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).containsIgnoringCase("test");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM professional_categories WHERE id = -71001", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM professionals WHERE id = -71002", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_schedules WHERE id IN (-71003, -71004)", Integer.class)).isZero();
        try {
            jdbc.update("INSERT INTO professional_categories VALUES (-71001, 'Salud', 'Private description', true, now(), now())");
            jdbc.update("INSERT INTO professionals VALUES (-71002, -71001, 'Ana', 'Gomez', false, now(), now())");
            jdbc.update("INSERT INTO weekly_schedules VALUES (-71003, -71002, 'MONDAY', '09:00', '12:00', 30, true, now(), now())");
            jdbc.update("INSERT INTO weekly_schedules VALUES (-71004, -71002, 'TUESDAY', '10:00', '13:00', 20, false, now(), now())");
            var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build();
            mvc.perform(get("/internal/api/professionals/-71002")).andExpect(status().isOk())
                    .andExpect(content().json("""
                        {"id":-71002,"firstName":"Ana","lastName":"Gomez","enabled":false,
                         "category":{"id":-71001,"name":"Salud","enabled":true},
                         "weeklySchedules":[
                           {"id":-71004,"dayOfWeek":"TUESDAY","startTime":"10:00:00","endTime":"13:00:00","slotDurationMinutes":20,"enabled":false},
                           {"id":-71003,"dayOfWeek":"MONDAY","startTime":"09:00:00","endTime":"12:00:00","slotDurationMinutes":30,"enabled":true}]}
                        """, JsonCompareMode.STRICT));
            verifyNoInteractions(snapshot, redis);
        } finally {
            jdbc.update("DELETE FROM weekly_schedules WHERE id IN (-71003, -71004)");
            jdbc.update("DELETE FROM professionals WHERE id = -71002");
            jdbc.update("DELETE FROM professional_categories WHERE id = -71001");
        }
    }
}
