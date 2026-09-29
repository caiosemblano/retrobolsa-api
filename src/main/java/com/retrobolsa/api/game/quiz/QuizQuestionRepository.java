package com.retrobolsa.api.game.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    /** Perguntas da aula já com as alternativas, numa query só. */
    @Query("""
            SELECT DISTINCT q FROM QuizQuestion q LEFT JOIN FETCH q.options
            WHERE q.articleId = :articleId
            ORDER BY q.displayOrder ASC
            """)
    List<QuizQuestion> findWithOptionsByArticleId(@Param("articleId") UUID articleId);

    boolean existsByArticleId(UUID articleId);

    /** Quantas perguntas tem cada aula que tem quiz, para a listagem não consultar aula por aula. */
    @Query("SELECT q.articleId AS articleId, COUNT(q) AS total FROM QuizQuestion q GROUP BY q.articleId")
    List<QuestionCount> countByArticle();

    interface QuestionCount {
        UUID getArticleId();
        long getTotal();
    }
}
