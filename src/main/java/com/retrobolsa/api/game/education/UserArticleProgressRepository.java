package com.retrobolsa.api.game.education;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserArticleProgressRepository extends JpaRepository<UserArticleProgress, UserArticleProgressId> {
    boolean existsByIdUserIdAndIdArticleId(UUID userId, UUID articleId);

    /** Todo o progresso do usuário numa query só, para a listagem não consultar artigo por artigo. */
    List<UserArticleProgress> findAllByIdUserId(UUID userId);
}
