package ar.edu.um.prog2.catalogsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CatalogSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogSyncApplication.class, args);
	}

}
