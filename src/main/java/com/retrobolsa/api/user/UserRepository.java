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
}
