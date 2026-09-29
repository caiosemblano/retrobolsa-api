package com.retrobolsa.api.game.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserQuizAttemptRepository extends JpaRepository<UserQuizAttempt, UUID> {

    /** A melhor nota do usuário em cada aula, numa query só. */
    @Query("""
            SELECT a.articleId AS articleId, MAX(a.score) AS bestScore FROM UserQuizAttempt a
            WHERE a.userId = :userId GROUP BY a.articleId
            """)
    List<BestScore> findBestScores(@Param("userId") UUID userId);

    long countByUserIdAndArticleId(UUID userId, UUID articleId);

    interface BestScore {
        UUID getArticleId();
        int getBestScore();
    }
}
