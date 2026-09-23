package com.retrobolsa.api.game.education;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserArticleProgressRepository extends JpaRepository<UserArticleProgress, UserArticleProgressId> {
    boolean existsByIdUserIdAndIdArticleId(UUID userId, UUID articleId);

    /** Todo o progresso do usuário numa query só, para a listagem não consultar artigo por artigo. */
    List<UserArticleProgress> findAllByIdUserId(UUID userId);

    long countByIdUserId(UUID userId);

    /** O progresso não tem relação JPA com o artigo (a chave guarda só UUIDs), daí a subquery. */
    @Query("""
            SELECT COUNT(p) FROM UserArticleProgress p
            WHERE p.id.userId = :userId
              AND p.id.articleId IN (SELECT a.id FROM Article a WHERE a.module.id = :moduleId)
            """)
    long countCompletedInModule(@Param("userId") UUID userId, @Param("moduleId") UUID moduleId);
}
