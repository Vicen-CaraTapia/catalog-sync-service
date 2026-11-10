package ar.edu.um.prog2.catalogsync.infrastructure.security.jwt;

import ar.edu.um.prog2.catalogsync.config.InternalCatalogJwtProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Project conventions, independent of cátedra and final-user token contracts. */
final class InternalCatalogJwtClaimValidator implements OAuth2TokenValidator<Jwt> {
    static final String SUBJECT = "appointment-reservation-service";
    private final InternalCatalogJwtProperties properties;

    InternalCatalogJwtClaimValidator(InternalCatalogJwtProperties properties) {
        this.properties = properties;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var claims = jwt.getClaims();
        boolean audienceValid = claims.get("aud") instanceof List<?> audience
                && audience.stream().allMatch(String.class::isInstance)
                && audience.contains(properties.audience());
        boolean valid = properties.issuer().equals(claims.get("iss"))
                && SUBJECT.equals(claims.get("sub"))
                && claims.get("exp") instanceof Instant
                && (!claims.containsKey("nbf") || claims.get("nbf") instanceof Instant)
                && (!claims.containsKey("scope") || claims.get("scope") instanceof String)
                && audienceValid;
        return valid ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid internal service JWT claims", null));
    }
}
