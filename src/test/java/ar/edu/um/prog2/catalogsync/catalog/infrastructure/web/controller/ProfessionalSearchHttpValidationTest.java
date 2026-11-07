package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.exception.ProfessionalSearchValidationExceptionHandler;
import ar.edu.um.prog2.catalogsync.infrastructure.security.config.CatalogSecurityConfiguration;
import ar.edu.um.prog2.catalogsync.infrastructure.security.jwt.InternalCatalogJwtConfiguration;
import ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/** Real HTTP dispatch detects sendError -> /error -> 403 regressions that isolated MVC misses. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ProfessionalSearchHttpValidationTest.HttpTestConfiguration.class)
@ActiveProfiles("test")
class ProfessionalSearchHttpValidationTest {
    @LocalServerPort private int port;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private SearchProfessionalsUseCase search;

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) {
        InternalJwtTestKeys.register(registry);
    }

    @ParameterizedTest
    @ValueSource(strings = {"availableOn=fecha-invalida", "availableOn=2026-02-30",
            "categoryId=abc", "enabled=texto", "page=abc", "size=abc",
            "page=-1", "size=0", "size=101"})
    void authorizedInvalidParametersReturn400ProblemWithoutErrorRedispatch(String query) throws Exception {
        String token = InternalJwtTestKeys.sign(InternalJwtTestKeys.claims());
        var response = request(query, token);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        assertThat(mapper.readTree(response.body())).isEqualTo(mapper.readTree("""
                {"status":400,"title":"Bad Request","detail":"Invalid professional search request.",
                 "instance":"/api/professionals"}
                """));
        verifyNoInteractions(search);
    }

    @Test
    void enablingTheRouteForThisTestStillRequiresAuthentication() throws Exception {
        assertThat(request("availableOn=fecha-invalida", null).statusCode()).isEqualTo(401);
        verifyNoInteractions(search);
    }

    private HttpResponse<String> request(String query, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                + "/api/professionals?" + query)).timeout(Duration.ofSeconds(10)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
    })
    @Import({ProfessionalSearchController.class, ProfessionalSearchValidationExceptionHandler.class,
            CatalogSecurityConfiguration.class, InternalCatalogJwtConfiguration.class})
    static class HttpTestConfiguration {
        // C4 remains denied in production. Only this test context authorizes its search GET.
        @Bean
        @Order(0)
        SecurityFilterChain searchForTestOnly(HttpSecurity http, JwtDecoder decoder) throws Exception {
            return http.securityMatcher("/api/professionals")
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAuthority("SCOPE_catalog:read"))
                    .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)))
                    .build();
        }
    }
}
