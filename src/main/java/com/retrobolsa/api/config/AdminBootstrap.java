package com.retrobolsa.api.config;

import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;

    @Value("${retrobolsa.admin.email:}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        userRepository.findByEmail(adminEmail.trim()).ifPresent(user -> {
            if (!"ADMIN".equals(user.getRole())) {
                user.setRole("ADMIN");
                userRepository.save(user);
            }
        });
    }
}
