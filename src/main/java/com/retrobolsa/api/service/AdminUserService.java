package com.retrobolsa.api.service;

import com.retrobolsa.api.exception.NotFoundException;
import com.retrobolsa.api.game.dto.AdminUserDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** O admin busca usuários e promove (ou rebaixa) professores. Não há autocadastro de professor. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String ADMIN = "ADMIN";
    private static final String TEACHER = "TEACHER";
    private static final Set<String> ASSIGNABLE = Set.of("PLAYER", TEACHER);
    private static final int MAX_RESULTS = 20;

    private final UserRepository userRepository;

    /** Com busca vazia, lista os professores atuais. */
    @Transactional(readOnly = true)
    public List<AdminUserDto> search(String query) {
        String q = query == null ? "" : query.trim();
        List<User> users = q.isEmpty()
                ? userRepository.findAllByRoleOrderByUsername(TEACHER)
                : userRepository.search(q, PageRequest.of(0, MAX_RESULTS));
        return users.stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminUserDto changeRole(UUID userId, String role) {
        String newRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ASSIGNABLE.contains(newRole)) {
            throw new IllegalArgumentException("Papel inválido: use PLAYER ou TEACHER.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        if (ADMIN.equals(user.getRole())) {
            throw new IllegalArgumentException("O papel de um administrador não muda por aqui.");
        }
        user.setRole(newRole);
        return toDto(userRepository.save(user));
    }

    private AdminUserDto toDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
