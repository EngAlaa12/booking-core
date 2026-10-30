package com.alaa.banking.accountservice.auth;

import com.alaa.banking.accountservice.user.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String status,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getStatus().name(), user.getCreatedAt());
    }
}
