package ar.edu.um.prog2.catalogsync.infrastructure.security.jwt;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Accepts only an external file URI containing a PEM X.509 SubjectPublicKeyInfo RSA key. */
final class InternalCatalogRsaPublicKeyLoader {
    private InternalCatalogRsaPublicKeyLoader() { }

    static RSAPublicKey load(String location) {
        try {
            var uri = URI.create(location);
            if (!"file".equals(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
            var pem = Files.readString(Path.of(uri)).strip();
            String begin = "-----BEGIN PUBLIC KEY-----";
            String end = "-----END PUBLIC KEY-----";
            if (!pem.startsWith(begin) || !pem.endsWith(end)) {
                throw new IllegalArgumentException();
            }
            var der = Base64.getDecoder().decode(
                    pem.substring(begin.length(), pem.length() - end.length()).replaceAll("\\s", ""));
            var key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            if (key.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException();
            }
            return key;
        } catch (Exception failure) {
            // Do not expose paths, key content or arbitrary provider/I/O messages during startup.
            throw new IllegalStateException("Internal JWT verification requires a valid external RSA public key (PEM SPKI, at least 2048 bits)");
        }
    }
}
