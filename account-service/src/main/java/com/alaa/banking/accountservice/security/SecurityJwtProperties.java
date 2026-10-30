package com.alaa.banking.accountservice.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record SecurityJwtProperties(
        String issuer,
        Duration accessTokenTtl,
        String privateKeyLocation,
        String publicKeyLocation
) {

    public SecurityJwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "account-service";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
    }
}
