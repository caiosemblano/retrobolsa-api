package com.retrobolsa.api.user;

import com.retrobolsa.api.user.dto.LoginRequest;
import com.retrobolsa.api.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor


public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public void register(RegisterRequest request) throws IllegalArgumentException {
        String email = request.getEmail();
        String username = request.getUsername();
        String passwordHash = passwordEncoder.encode(request.getSenha());

        if (userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("Este email ja está cadastrado");
        }

        if (userRepository.existsByUsername(username)){
            throw new IllegalArgumentException("Este usuário já está cadastrado");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .build();
        userRepository.save(user);

    }

    public void login(LoginRequest request) throws IllegalArgumentException{

    }

}
