package ar.edu.um.prog2.catalogsync;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.CatalogSyncStateRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalCategoryJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.ProfessionalJpaRepository;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.persistence.repository.WeeklyScheduleJpaRepository;
import ar.edu.um.prog2.catalogsync.synchronization.domain.ports.in.CatalogUpdateNotificationUseCase;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.listener.CatalogUpdatedListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
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

	@Test
	void apiPathsAreDeniedUntilInternalJwtSecurityIsImplemented(@Autowired RestTestClient restClient) {
		restClient.get()
				.uri("/api/professionals")
				.exchange()
				.expectStatus().isForbidden();
		restClient.get()
				.uri("/api/professionals/10")
				.exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void catalogUpdatedListenerIsRegisteredButInactiveWithTheOrchestrationUseCase(
			@Autowired ApplicationContext context,
			@Autowired KafkaListenerEndpointRegistry registry
	) {
		assertThat(context.getBeansOfType(CatalogUpdateNotificationUseCase.class)).hasSize(1);
		var container = registry.getListenerContainer(CatalogUpdatedListener.LISTENER_ID);
		assertThat(container).isNotNull();
		assertThat(container.isRunning()).isFalse();
	}

}
