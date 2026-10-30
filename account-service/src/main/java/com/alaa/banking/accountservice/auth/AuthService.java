package com.alaa.banking.accountservice.auth;

import com.alaa.banking.accountservice.common.error.InvalidCredentialsException;
import com.alaa.banking.accountservice.security.IssuedToken;
import com.alaa.banking.accountservice.security.TokenService;
import com.alaa.banking.accountservice.user.User;
import com.alaa.banking.accountservice.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        IssuedToken token = tokenService.issueAccessToken(user);
        long expiresInSeconds = Duration.between(Instant.now(), token.expiresAt()).getSeconds();
        return new LoginResponse(token.accessToken(), "Bearer", expiresInSeconds);
    }
}
