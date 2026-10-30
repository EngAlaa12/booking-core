package com.alaa.banking.accountservice.auth;

import com.alaa.banking.accountservice.common.error.InvalidCredentialsException;
import com.alaa.banking.accountservice.user.User;
import com.alaa.banking.accountservice.user.UserRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterService registerService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(RegisterService registerService, AuthService authService, UserRepository userRepository) {
        this.registerService = registerService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return UserResponse.from(user);
    }
}
