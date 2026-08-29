package com.retrobolsa.api.game.education;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserArticleProgressRepository extends JpaRepository<UserArticleProgress, UserArticleProgressId> {
    boolean existsByIdUserIdAndIdArticleId(UUID userId, UUID articleId);
}
