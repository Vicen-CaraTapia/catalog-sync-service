package ar.edu.um.prog2.catalogsync.config;

/** Bound explicitly by the servlet security configuration, not by the global properties scan. */
public record InternalCatalogJwtProperties(String publicKeyLocation, String issuer, String audience) {
    public static final String PREFIX = "catalog.security.internal-jwt";

    public InternalCatalogJwtProperties {
        if (publicKeyLocation == null || publicKeyLocation.isBlank()
                || issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("Internal JWT public key location, issuer and audience are required");
        }
    }
}
