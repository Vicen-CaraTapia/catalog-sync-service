package ar.edu.um.prog2.catalogsynctest.security;

import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Per-JVM ephemeral key; only its public part is written to a temporary PEM file. */
public final class InternalJwtTestKeys {
    public static final KeyPair KEY_PAIR = generate();
    public static final String PUBLIC_KEY_LOCATION = writePublicKey();
    private InternalJwtTestKeys() { }

    public static KeyPair generate() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate ephemeral test key", e);
        }
    }

    private static String writePublicKey() {
        try {
            var path = Files.createTempFile("catalog-jwt-test-", ".pem");
            path.toFile().deleteOnExit();
            Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n");
            return path.toUri().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create ephemeral test public key", e);
        }
    }

    public static java.util.Map<String, Object> claims() {
        var claims = new java.util.HashMap<String, Object>();
        claims.put("iss", "appointment-reservation-service");
        claims.put("sub", "appointment-reservation-service");
        claims.put("aud", java.util.List.of("catalog-sync-service"));
        claims.put("exp", java.time.Instant.now().plusSeconds(600).getEpochSecond());
        claims.put("scope", "catalog:read");
        return claims;
    }

    public static String sign(java.util.Map<String, Object> claims) throws Exception {
        var jwt = new com.nimbusds.jose.JWSObject(
                new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256),
                new com.nimbusds.jose.Payload(claims));
        jwt.sign(new com.nimbusds.jose.crypto.RSASSASigner(
                (java.security.interfaces.RSAPrivateKey) KEY_PAIR.getPrivate()));
        return jwt.serialize();
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("catalog.security.internal-jwt.public-key-location", () -> PUBLIC_KEY_LOCATION);
        registry.add("catalog.security.internal-jwt.issuer", () -> "appointment-reservation-service");
        registry.add("catalog.security.internal-jwt.audience", () -> "catalog-sync-service");
    }
}
