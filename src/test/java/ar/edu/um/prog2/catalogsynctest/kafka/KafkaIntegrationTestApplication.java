package ar.edu.um.prog2.catalogsynctest.kafka;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.config.CatedraKafkaConfiguration;
import ar.edu.um.prog2.catalogsync.synchronization.infrastructure.kafka.verification.CatedraKafkaConnectivityVerifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
@EnableConfigurationProperties(CatedraIntegrationProperties.class)
@Import({CatedraKafkaConfiguration.class, CatedraKafkaConnectivityVerifier.class})
public class KafkaIntegrationTestApplication {
}
