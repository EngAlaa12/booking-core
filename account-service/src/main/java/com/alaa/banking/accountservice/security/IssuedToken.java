package com.alaa.banking.accountservice.security;

import java.time.Instant;

public record IssuedToken(String accessToken, Instant expiresAt) {
}
