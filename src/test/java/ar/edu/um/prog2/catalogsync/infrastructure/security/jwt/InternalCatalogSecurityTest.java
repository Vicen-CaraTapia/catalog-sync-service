package ar.edu.um.prog2.catalogsync.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.Professional;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.catalogsync.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.prog2.catalogsync.catalog.domain.ports.in.GetProfessionalDetailsUseCase;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller.InternalProfessionalDetailsController;
import ar.edu.um.prog2.catalogsync.infrastructure.security.config.CatalogSecurityConfiguration;
import ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Uses the production chains and decoder with genuine signatures; no mock authentication. */
@org.springframework.boot.autoconfigure.ImportAutoConfiguration({
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration.class
})
@WebMvcTest(InternalProfessionalDetailsController.class)
@Import({CatalogSecurityConfiguration.class, InternalCatalogJwtConfiguration.class})
class InternalCatalogSecurityTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private GetProfessionalDetailsUseCase useCase;

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) { InternalJwtTestKeys.register(registry); }

    @Test
    void anonymousRequestIsUnauthorizedWithBearerChallenge() throws Exception {
        var result = mvc.perform(get("/internal/api/professionals/10"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Internal service authentication is required.")).andReturn();
        assertNoSessionOrCookie(result);
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer private-marker-bad-token", "Bearer a.b.c", "Basic private-marker-credentials"})
    void malformedCredentialsNeverLeakInError(String authorization) throws Exception {
        var result = mvc.perform(get("/internal/api/professionals/10").header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("private-marker", authorization, "Exception", "stackTrace");
        verifyNoInteractions(useCase);
    }

    static Stream<Arguments> invalidClaims() {
        return Stream.of(Arguments.of("exp", Instant.now().minusSeconds(120).getEpochSecond()),
                Arguments.of("exp", null), Arguments.of("iss", "catedra-private-marker"),
                Arguments.of("iss", null), Arguments.of("aud", List.of("another-service")),
                Arguments.of("aud", null), Arguments.of("sub", "final-user-private-marker"),
                Arguments.of("sub", null), Arguments.of("scope", List.of("catalog:read")),
                Arguments.of("nbf", Instant.now().plusSeconds(120).getEpochSecond()));
    }

    @ParameterizedTest
    @MethodSource("invalidClaims")
    void invalidSignedTokenIsUnauthorizedBeforeTheUseCase(String claim, Object value) throws Exception {
        var claims = InternalJwtTestKeys.claims();
        if (value == null) { claims.remove(claim); } else { claims.put(claim, value); }
        String token = InternalJwtTestKeys.sign(claims);
        var result = mvc.perform(get("/internal/api/professionals/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Internal service authentication is required.")).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(token, "private-marker", "Exception");
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "other", "catalog:reader", "catalog:read:all", "prefix-catalog:read"})
    void authenticatedWithoutExactScopeIsForbidden(String scope) throws Exception {
        var claims = InternalJwtTestKeys.claims(); claims.put("scope", scope);
        forbidden(claims);
    }

    @Test
    void missingScopeAndAlternativeScpClaimCannotGrantPermission() throws Exception {
        var claims = InternalJwtTestKeys.claims(); claims.remove("scope");
        claims.put("scp", List.of("catalog:read"));
        forbidden(claims);
    }

    private void forbidden(Map<String, Object> claims) throws Exception {
        String token = InternalJwtTestKeys.sign(claims);
        var result = mvc.perform(get("/internal/api/professionals/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Access to this internal operation is denied.")).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(token, "Exception", "scope");
        assertNoSessionOrCookie(result);
        verifyNoInteractions(useCase);
    }

    @Test
    void exactScopeReachesTheControllerAndKeepsDisabledProfessionalVisible() throws Exception {
        when(useCase.getById(10)).thenReturn(new ProfessionalDetails(
                new Professional(10L, 20L, "Ana", "Gomez", false, Instant.EPOCH, Instant.EPOCH),
                new ProfessionalCategory(20L, "Salud", "Not exposed", true, Instant.EPOCH, Instant.EPOCH), List.of()));
        var result = mvc.perform(get("/internal/api/professionals/10")
                        .header("Authorization", "Bearer " + InternalJwtTestKeys.sign(InternalJwtTestKeys.claims())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.category.description").doesNotExist())
                .andExpect(jsonPath("$.weeklySchedules").isEmpty()).andReturn();
        assertNoSessionOrCookie(result);
        verify(useCase).getById(10);
    }

    @Test
    void authenticatedMissingProfessionalRetainsTheC71ProblemResponse() throws Exception {
        when(useCase.getById(999)).thenThrow(new ProfessionalNotFoundException(999));
        mvc.perform(get("/internal/api/professionals/999")
                        .header("Authorization", "Bearer " + InternalJwtTestKeys.sign(InternalJwtTestKeys.claims())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "HEAD"})
    void otherInternalMethodsRemainDeniedEvenWithReadPermission(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/internal/api/professionals/10")
                        .header("Authorization", "Bearer " + InternalJwtTestKeys.sign(InternalJwtTestKeys.claims())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/internal/api/other", "/internal/api/professionals", "/internal/api/professionals/10/extra"})
    void otherInternalRoutesRemainDenied(String path) throws Exception {
        mvc.perform(get(path).header("Authorization", "Bearer " + InternalJwtTestKeys.sign(InternalJwtTestKeys.claims())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    void existingSessionAuthenticationCannotReplaceTheInternalBearer() throws Exception {
        var session = new MockHttpSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated("appointment", "unused",
                List.of(new SimpleGrantedAuthority("SCOPE_catalog:read")));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        mvc.perform(get("/internal/api/professionals/10").session(session)).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/professionals", "/api/professionals/10", "/unknown"})
    void internalTokenDoesNotOpenOtherApplicationRoutes(String path) throws Exception {
        mvc.perform(get(path).header("Authorization", "Bearer " + InternalJwtTestKeys.sign(InternalJwtTestKeys.claims())))
                .andExpect(status().isForbidden());
    }

    private static void assertNoSessionOrCookie(MvcResult result) {
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    }
}
