package com.retrobolsa.api.game.practice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PracticeRunRepository extends JpaRepository<PracticeRun, UUID> {

    /** Quantas vezes o jogador treinou em cada rodada e o melhor resultado dele em cada uma. */
    @Query("""
            SELECT r.competitionId AS competitionId, COUNT(r) AS runs, MAX(r.totalReturn) AS bestReturn
            FROM PracticeRun r WHERE r.userId = :userId GROUP BY r.competitionId
            """)
    List<Summary> summarize(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    interface Summary {
        UUID getCompetitionId();
        long getRuns();
        BigDecimal getBestReturn();
    }
}
