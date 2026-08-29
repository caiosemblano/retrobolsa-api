package com.retrobolsa.api.config;

import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${retrobolsa.admin.email:retroadm@gmail.com}")
    private String adminEmail;

    @Value("${retrobolsa.admin.password:Admin1234}")
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
                User admin = User.builder()
                        .username("Administrador")
                        .email(email)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .role("ADMIN")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(admin);
            }
        );
    }
}
