package com.retrobolsa.api.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);
    User findByUsername(String username);

    /**
     * Ranking global paginado, excluindo uma role (tipicamente "ADMIN") — contas
     * administrativas não são jogadores reais e não devem aparecer no ranking.
     */
    List<User> findAllByRoleNotOrderByTotalScoreDescUsernameAsc(String role, Pageable pageable);

    /** Quantos usuários (fora da role informada) têm score estritamente maior. */
    long countByTotalScoreGreaterThanAndRoleNot(int totalScore, String role);

    /** Desempate: quantos usuários (fora da role informada) com o mesmo score vêm antes por username. */
    long countByTotalScoreAndUsernameLessThanAndRoleNot(int totalScore, String username, String role);

    /** Total de usuários (fora da role informada) — usado para "de quantos jogadores" no ranking. */
    long countByRoleNot(String role);
}
