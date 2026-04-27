package com.retrobolsa.api.user;

import com.retrobolsa.api.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor


public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public void register(RegisterRequest request) throws Exception {
        String email = request.getEmail();
        String username = request.getUsername();
        String passwordHash = passwordEncoder.encode(request.getSenha());

        if (userRepository.existsByEmail(email)){
            throw new Exception("Este email ja esta cadastrado");
        }

        if (userRepository.existsByUsername(username)){
            throw new Exception("Este usuario/username ja esta cadastrado");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .build();
        userRepository.save(user);

    }

}
