package ar.edu.um.prog2.catalogsync.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.catalogsync.config.InternalCatalogJwtProperties;
import ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

class InternalCatalogJwtDecoderTest {
    private static final KeyPair OTHER_KEY = InternalJwtTestKeys.generate();
    private final NimbusJwtDecoder decoder = new InternalCatalogJwtConfiguration().internalCatalogJwtDecoder(
            new InternalCatalogJwtProperties(InternalJwtTestKeys.PUBLIC_KEY_LOCATION,
                    "appointment-reservation-service", "catalog-sync-service"));

    @Test
    void verifiesGenuinelySignedRs256AndExactReadAuthority() throws Exception {
        var jwt = decoder.decode(sign(claims()));
        assertThat(jwt.getSubject()).isEqualTo("appointment-reservation-service");
        assertThat(new JwtGrantedAuthoritiesConverter().convert(jwt))
                .extracting(Object::toString).containsExactly("SCOPE_catalog:read");
    }

    @Test
    void rejectsAValidSignatureFromAnotherIdentitysKey() throws Exception {
        String token = sign(claims(), OTHER_KEY, JWSAlgorithm.RS256);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HS256", "HS512", "RS512"})
    void rejectsAlgorithmSubstitutionIncludingFinalUserHs512(String algorithm) throws Exception {
        var object = new JWSObject(new JWSHeader(JWSAlgorithm.parse(algorithm)), new Payload(claims()));
        if (algorithm.startsWith("HS")) {
            byte[] secret = new byte[64];
            new SecureRandom().nextBytes(secret);
            object.sign(new MACSigner(secret));
        } else {
            object.sign(new RSASSASigner((RSAPrivateKey) InternalJwtTestKeys.KEY_PAIR.getPrivate()));
        }
        String token = object.serialize();
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsUnsignedAndMalformedTokens() {
        String unsigned = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes())
                + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes()) + ".";
        assertThatThrownBy(() -> decoder.decode(unsigned)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"exp", "iss", "aud", "sub"})
    void rejectsMissingMandatoryClaim(String claim) throws Exception {
        var values = claims(); values.remove(claim);
        String token = sign(values);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    static Stream<Arguments> invalidClaims() {
        return Stream.of(
                Arguments.of("iss", "catedra"), Arguments.of("iss", 23),
                Arguments.of("sub", "final-user-123"), Arguments.of("sub", List.of("appointment-reservation-service")),
                Arguments.of("aud", List.of("appointment-reservation-service")), Arguments.of("aud", 42),
                Arguments.of("exp", Instant.now().minusSeconds(120).getEpochSecond()), Arguments.of("exp", "not-a-date"),
                Arguments.of("nbf", Instant.now().plusSeconds(120).getEpochSecond()), Arguments.of("nbf", "not-a-date"),
                Arguments.of("scope", List.of("catalog:read")), Arguments.of("scope", 42));
    }

    @ParameterizedTest
    @MethodSource("invalidClaims")
    void rejectsInvalidSecurityClaims(String name, Object value) throws Exception {
        var values = claims(); values.put(name, value);
        String token = sign(values);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "catalog:read:all", "prefix-catalog:read", "catalog:reader"})
    void validScopeWithoutExactPermissionCannotGrantRead(String scope) throws Exception {
        var values = claims(); values.put("scope", scope);
        var jwt = decoder.decode(sign(values));
        assertThat(new JwtGrantedAuthoritiesConverter().convert(jwt))
                .extracting(Object::toString).doesNotContain("SCOPE_catalog:read");
    }

    @Test
    void missingScopeAuthenticatesButDoesNotGrantPermission() throws Exception {
        var values = claims(); values.remove("scope");
        assertThat(new JwtGrantedAuthoritiesConverter().convert(decoder.decode(sign(values)))).isEmpty();
    }

    @Test
    void supportsMultipleExactScopesAndAudienceEntries() throws Exception {
        var values = claims(); values.put("scope", "other catalog:read");
        values.put("aud", List.of("another-resource", "catalog-sync-service"));
        assertThat(new JwtGrantedAuthoritiesConverter().convert(decoder.decode(sign(values))))
                .extracting(Object::toString).contains("SCOPE_catalog:read");
    }

    @Test
    void permitsOnlyTheDocumentedSmallClockSkew() throws Exception {
        var values = claims(); values.put("exp", Instant.now().minusSeconds(10).getEpochSecond());
        assertThat(decoder.decode(sign(values))).isNotNull();
        values = claims(); values.put("nbf", Instant.now().plusSeconds(10).getEpochSecond());
        assertThat(decoder.decode(sign(values))).isNotNull();
    }

    @Test
    void claimRejectionDoesNotExposeTokenOrClaimContents() throws Exception {
        var values = claims(); values.put("sub", "private-marker-never-log");
        String token = sign(values);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class)
                .hasMessageNotContaining(token).hasMessageNotContaining("private-marker-never-log");
    }

    private static Map<String, Object> claims() {
        var values = new HashMap<String, Object>();
        values.put("iss", "appointment-reservation-service");
        values.put("sub", "appointment-reservation-service");
        values.put("aud", List.of("catalog-sync-service"));
        values.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
        values.put("scope", "catalog:read");
        return values;
    }

    private static String sign(Map<String, Object> values) throws Exception {
        return sign(values, InternalJwtTestKeys.KEY_PAIR, JWSAlgorithm.RS256);
    }

    private static String sign(Map<String, Object> values, KeyPair pair, JWSAlgorithm algorithm) throws Exception {
        var object = new JWSObject(new JWSHeader(algorithm), new Payload(values));
        object.sign(new RSASSASigner((RSAPrivateKey) pair.getPrivate()));
        return object.serialize();
    }
}
