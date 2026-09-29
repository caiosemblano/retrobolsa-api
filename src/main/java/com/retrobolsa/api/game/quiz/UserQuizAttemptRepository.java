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

    /** Em quantas aulas diferentes o usuário já tirou nota máxima. */
    @Query("SELECT COUNT(DISTINCT a.articleId) FROM UserQuizAttempt a WHERE a.userId = :userId AND a.score = a.total")
    long countPerfectQuizzes(@Param("userId") UUID userId);

    interface BestScore {
        UUID getArticleId();
        int getBestScore();
    }
}
