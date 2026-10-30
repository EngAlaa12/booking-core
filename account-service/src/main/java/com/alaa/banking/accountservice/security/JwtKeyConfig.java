package com.alaa.banking.accountservice.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

/**
 * Loads (or generates) the RSA key pair used to sign and verify access tokens issued by this
 * service, and exposes the {@link JwtEncoder} / {@link JwtDecoder} beans built from it.
 *
 * <p>If {@code security.jwt.private-key-location} and {@code security.jwt.public-key-location}
 * are both set, PEM-encoded PKCS8 / X509 keys are loaded from those locations (e.g.
 * {@code file:/run/secrets/jwt-private.pem} or {@code classpath:certs/jwt-private.pem}).
 * Otherwise an ephemeral RSA key pair is generated at startup; this is fine for local
 * development but means tokens will not survive a restart, and multiple instances would not
 * agree on signatures, so production configuration must supply real key locations.
 */
@Configuration
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    @Bean
    RSAKey rsaKey(SecurityJwtProperties properties, ResourceLoader resourceLoader) {
        if (StringUtils.hasText(properties.privateKeyLocation()) && StringUtils.hasText(properties.publicKeyLocation())) {
            return loadConfiguredRsaKey(properties, resourceLoader);
        }
        log.warn("security.jwt.private-key-location / public-key-location are not configured; "
                + "generating an ephemeral RSA key pair for this run. Tokens will not be valid "
                + "across restarts or other instances. Configure real key locations for anything "
                + "beyond local development.");
        return generateEphemeralRsaKey();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, context) -> jwkSelector.select(jwkSet);
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey) throws java.security.NoSuchAlgorithmException,
            com.nimbusds.jose.JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    private RSAKey loadConfiguredRsaKey(SecurityJwtProperties properties, ResourceLoader resourceLoader) {
        try {
            RSAPrivateKey privateKey = readPrivateKey(resourceLoader.getResource(properties.privateKeyLocation()));
            RSAPublicKey publicKey = readPublicKey(resourceLoader.getResource(properties.publicKeyLocation()));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load configured JWT RSA key pair from "
                    + properties.privateKeyLocation() + " / " + properties.publicKeyLocation(), e);
        }
    }

    private RSAPrivateKey readPrivateKey(Resource resource) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(stripPem(resource, "PRIVATE KEY"));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private RSAPublicKey readPublicKey(Resource resource) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(stripPem(resource, "PUBLIC KEY"));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String stripPem(Resource resource, String marker) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return content
                    .replace("-----BEGIN " + marker + "-----", "")
                    .replace("-----END " + marker + "-----", "")
                    .replaceAll("\\s", "");
        }
    }

    private RSAKey generateEphemeralRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate an ephemeral RSA key pair", e);
        }
    }
}
