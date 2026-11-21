package ar.edu.um.prog2.catalogsync.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import ar.edu.um.prog2.catalogsync.config.InternalCatalogJwtProperties;
import ar.edu.um.prog2.catalogsynctest.security.InternalJwtTestKeys;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class InternalCatalogJwtConfigurationTest {
    @TempDir Path temporary;

    private WebApplicationContextRunner context(String location) {
        return new WebApplicationContextRunner().withUserConfiguration(InternalCatalogJwtConfiguration.class)
                .withPropertyValues("catalog.security.internal-jwt.public-key-location=" + location,
                        "catalog.security.internal-jwt.issuer=appointment-reservation-service",
                        "catalog.security.internal-jwt.audience=catalog-sync-service");
    }

    @Test
    void startsWithExternalSpkiPublicKeyAndTypedProperties() {
        context(InternalJwtTestKeys.PUBLIC_KEY_LOCATION).run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c).hasSingleBean(JwtDecoder.class);
            assertThat(c).hasSingleBean(InternalCatalogJwtProperties.class);
        });
    }

    @Test
    void missingConfigurationFailsStartup() {
        new WebApplicationContextRunner().withUserConfiguration(InternalCatalogJwtConfiguration.class)
                .run(c -> assertThat(c).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"issuer", "audience", "public-key-location"})
    void blankRequiredPropertyFailsStartup(String name) {
        context(InternalJwtTestKeys.PUBLIC_KEY_LOCATION)
                .withPropertyValues("catalog.security.internal-jwt." + name + "=")
                .run(c -> assertThat(c).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"file:/no-such-internal-key.pem", "classpath:key.pem", "https://example.invalid/key.pem", "not a URI"})
    void unsupportedOrMissingKeyFailsStartupWithSanitizedLoaderError(String location) {
        context(location).run(c -> assertThat(c).hasFailed());
        assertThatThrownBy(() -> InternalCatalogRsaPublicKeyLoader.load(location))
                .isInstanceOf(IllegalStateException.class).hasNoCause().hasMessageNotContaining(location);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRIVATE KEY", "RSA PUBLIC KEY", "CERTIFICATE", "PUBLIC KEY"})
    void rejectsUnsupportedPemOrCorruptData(String label) throws Exception {
        var path = temporary.resolve("invalid.pem");
        Files.writeString(path, "-----BEGIN " + label + "-----\nprivate-marker\n-----END " + label + "-----");
        context(path.toUri().toString()).run(c -> assertThat(c).hasFailed());
        assertThatThrownBy(() -> InternalCatalogRsaPublicKeyLoader.load(path.toUri().toString()))
                .hasNoCause().hasMessageNotContaining("private-marker");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RSA", "EC"})
    void rejectsWeakRsaAndNonRsaKeys(String algorithm) throws Exception {
        var generator = KeyPairGenerator.getInstance(algorithm);
        generator.initialize(algorithm.equals("RSA") ? 1024 : 256);
        var path = temporary.resolve("wrong-kind.pem");
        Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(generator.generateKeyPair().getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----");
        context(path.toUri().toString()).run(c -> assertThat(c).hasFailed());
    }
}
