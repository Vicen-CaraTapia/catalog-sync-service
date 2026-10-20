package ar.edu.um.prog2.catalogsync;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
						+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
						+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
		}
)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
class CatalogSyncApplicationTests {

	@MockitoBean
	private ProfessionalCategoryJpaRepository professionalCategoryRepository;

	@MockitoBean
	private ProfessionalJpaRepository professionalRepository;

	@MockitoBean
	private WeeklyScheduleJpaRepository weeklyScheduleRepository;

	@MockitoBean
	private CatalogSyncStateRepository catalogSyncStateRepository;

	@Test
	void healthIsUp(@Autowired RestTestClient restClient) {
		restClient.get()
				.uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("UP");
	}

}
