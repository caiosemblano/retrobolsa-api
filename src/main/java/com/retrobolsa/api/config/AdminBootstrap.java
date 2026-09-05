package com.retrobolsa.api.config;

import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${retrobolsa.admin.email:retroadm@gmail.com}")
    private String adminEmail;

    @Value("${retrobolsa.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        String email = adminEmail.trim();
        userRepository.findByEmail(email).ifPresentOrElse(
            user -> {
                if (!"ADMIN".equals(user.getRole())) {
                    user.setRole("ADMIN");
                    userRepository.save(user);
                }
            },
            () -> {
                String password = resolveAdminPassword();
                User admin = User.builder()
                        .username("Administrador")
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .role("ADMIN")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(admin);
            }
        );
    }

    private String resolveAdminPassword() {
        if (adminPassword != null && !adminPassword.isBlank()) {
            return adminPassword;
        }
        byte[] randomBytes = new byte[18];
        secureRandom.nextBytes(randomBytes);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        log.warn("RETROBOLSA_ADMIN_PASSWORD não definida. Senha de admin gerada para '{}': {}", adminEmail, generated);
        return generated;
    }
}
