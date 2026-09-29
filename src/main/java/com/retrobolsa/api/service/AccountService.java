package com.retrobolsa.api.service;

import com.retrobolsa.api.exception.NotFoundException;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Senha sem e-mail: o jogador troca a própria senha logado; quem esqueceu pede ao
 * admin, que gera uma senha temporária (mostrada uma vez só) e o app obriga a
 * trocá-la no próximo acesso.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    /** Sem os caracteres que se confundem ao ditar ou copiar (0/o, 1/l/i). */
    static final String TEMPORARY_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    static final int TEMPORARY_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(User user, String current, String next, String confirmation) {
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        if (!next.equals(confirmation)) {
            throw new IllegalArgumentException("A confirmação é diferente da nova senha.");
        }
        if (passwordEncoder.matches(next, user.getPasswordHash())) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da atual.");
        }
        user.setPasswordHash(passwordEncoder.encode(next));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    /** @return a senha temporária, que não fica guardada em lugar nenhum além do hash */
    @Transactional
    public String resetPassword(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("A senha de um administrador não é redefinida por aqui.");
        }
        String temporary = temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        userRepository.save(user);
        return temporary;
    }

    static String temporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_LENGTH);
        for (int i = 0; i < TEMPORARY_LENGTH; i++) {
            password.append(TEMPORARY_ALPHABET.charAt(RANDOM.nextInt(TEMPORARY_ALPHABET.length())));
        }
        return password.toString();
    }
}
