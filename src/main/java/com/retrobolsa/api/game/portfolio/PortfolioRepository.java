package com.retrobolsa.api.game.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByUserIdAndCompetitionId(UUID userId, UUID competitionId);

    Optional<Portfolio> findTopByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<Portfolio> findByCompetitionIdOrderByTotalReturnDesc(UUID competitionId);

    List<Portfolio> findByCompetitionIdOrderByRankAsc(UUID competitionId);

    List<Portfolio> findByCompetitionRoundNumberOrderByRankAsc(int roundNumber);

    List<Portfolio> findByUserIdOrderByRankAsc(UUID userId);

    /**
     * Busca portfólios de uma competição ordenados por critérios de desempate:
     * 1º rank ASC (nulos por último), 2º totalReturn DESC, 3º submittedAt ASC.
     * Útil para rankings em que rank ainda não foi computado (ex: rodada open).
     */
    @Query("""
            SELECT p FROM Portfolio p
            WHERE p.competition.id = :competitionId
            ORDER BY
              CASE WHEN p.rank IS NULL THEN 1 ELSE 0 END ASC,
              p.rank ASC,
              p.totalReturn DESC NULLS LAST,
              p.submittedAt ASC
            """)
    List<Portfolio> findByCompetitionIdOrderByRankThenTieBreak(@Param("competitionId") UUID competitionId);

    /**
     * Busca portfólios de uma rodada (roundNumber) com mesmos critérios de desempate.
     */
    @Query("""
            SELECT p FROM Portfolio p
            WHERE p.competition.roundNumber = :roundNumber
            ORDER BY
              CASE WHEN p.rank IS NULL THEN 1 ELSE 0 END ASC,
              p.rank ASC,
              p.totalReturn DESC NULLS LAST,
              p.submittedAt ASC
            """)
    List<Portfolio> findByRoundNumberOrderByRankThenTieBreak(@Param("roundNumber") int roundNumber);

    /** Conta quantas competições um usuário participou. */
    long countByUserId(UUID userId);
}
