package com.alaa.banking.accountservice.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
