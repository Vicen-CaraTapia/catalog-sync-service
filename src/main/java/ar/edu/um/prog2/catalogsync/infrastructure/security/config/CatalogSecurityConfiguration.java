package ar.edu.um.prog2.catalogsync.infrastructure.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Keeps the HTTP surface closed until the internal Appointment-to-Catalog JWT
 * contract is implemented in C7. The cátedra technical JWT is intentionally
 * not used for this boundary.
 * Esto es seguridad temporal hasta que en c7 aplique JWT
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CatalogSecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().denyAll())
				.build();
	}

}
