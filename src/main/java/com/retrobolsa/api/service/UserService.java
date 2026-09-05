package com.retrobolsa.api.service;

import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import com.retrobolsa.api.user.dto.LoginRequest;
import com.retrobolsa.api.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.retrobolsa.api.security.JwtUtil;


@Service
@RequiredArgsConstructor
public class UserService {

    /** Qualquer e-mail cadastrado sob este domínio vira ADMIN automaticamente ao se registrar. */
    private static final String ADMIN_EMAIL_DOMAIN = "@admin.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    public void register(RegisterRequest request) throws IllegalArgumentException {
        String email = request.getEmail();
        String username = request.getUsername();
        String passwordHash = passwordEncoder.encode(request.getSenha());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este email ja está cadastrado");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Este usuário já está cadastrado");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(isAdminEmail(email) ? "ADMIN" : "PLAYER")
                .build();
        userRepository.save(user);

    }

    private boolean isAdminEmail(String email) {
        return email != null && email.toLowerCase().endsWith(ADMIN_EMAIL_DOMAIN);
    }

    public String login(LoginRequest request) throws IllegalArgumentException {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getSenha(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }
        return jwtUtil.generateToken(request.getEmail());
    }
}
