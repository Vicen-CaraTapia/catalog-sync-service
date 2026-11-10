package ar.edu.um.prog2.catalogsync.infrastructure.security.jwt;

import ar.edu.um.prog2.catalogsync.config.InternalCatalogJwtProperties;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InternalCatalogJwtConfiguration {
    // Accepts up to 30 seconds of clock difference when checking exp and nbf between backend hosts.
    static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

    @Bean
    InternalCatalogJwtProperties internalCatalogJwtProperties(Environment environment) {
        return Binder.get(environment).bind(InternalCatalogJwtProperties.PREFIX,
                Bindable.of(InternalCatalogJwtProperties.class))
                .orElseThrow(() -> new IllegalStateException("Internal JWT configuration is required"));
    }

    @Bean
    NimbusJwtDecoder internalCatalogJwtDecoder(InternalCatalogJwtProperties properties) {
        var decoder = NimbusJwtDecoder.withPublicKey(InternalCatalogRsaPublicKeyLoader.load(properties.publicKeyLocation()))
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new InternalCatalogJwtClaimValidator(properties), new JwtTimestampValidator(CLOCK_SKEW)));
        return decoder;
    }
}
