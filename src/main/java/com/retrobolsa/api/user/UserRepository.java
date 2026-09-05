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

    /** Retorna todos os usuários ordenados por totalScore desc e username asc (sem paginação). */
    List<User> findAllByOrderByTotalScoreDescUsernameAsc();

    /** Retorna usuários ordenados por totalScore desc e username asc com suporte a paginação/top-N. */
    List<User> findAllByOrderByTotalScoreDescUsernameAsc(Pageable pageable);

    /** Quantos usuários têm score estritamente maior (usado para calcular a posição global sem carregar a tabela toda). */
    long countByTotalScoreGreaterThan(int totalScore);

    /** Desempate: quantos usuários com o mesmo score vêm antes por ordem alfabética de username. */
    long countByTotalScoreAndUsernameLessThan(int totalScore, String username);
}
