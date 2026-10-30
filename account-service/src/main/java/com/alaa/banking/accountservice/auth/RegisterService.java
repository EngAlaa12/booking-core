package com.alaa.banking.accountservice.auth;

import com.alaa.banking.accountservice.common.error.EmailAlreadyRegisteredException;
import com.alaa.banking.accountservice.user.User;
import com.alaa.banking.accountservice.user.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User user = new User(email, passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
