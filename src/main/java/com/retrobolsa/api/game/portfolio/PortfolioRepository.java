package com.retrobolsa.api.game.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByUserIdAndCompetitionId(UUID userId, UUID competitionId);

    Optional<Portfolio> findTopByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<Portfolio> findByCompetitionIdOrderByTotalReturnDesc(UUID competitionId);

    List<Portfolio> findByCompetitionIdOrderByRankAsc(UUID competitionId);

    List<Portfolio> findByCompetitionRoundNumberOrderByRankAsc(int roundNumber);

    /**
     * Histórico de portfólios do usuário. O {@code JOIN FETCH} da competição evita
     * um N+1 no perfil, que lê roundNumber/scenarioTitle de cada portfólio.
     */
    @Query("""
            SELECT p FROM Portfolio p
            JOIN FETCH p.competition
            WHERE p.user.id = :userId
            ORDER BY p.rank ASC
            """)
    List<Portfolio> findByUserIdOrderByRankAsc(@Param("userId") UUID userId);

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

    /**
     * Portfólios já simulados dentro de uma faixa de rodadas (uma temporada),
     * excluindo contas administrativas. Ordenados por rodada para que a pontuação
     * da temporada seja acumulada na mesma ordem cronológica do score vitalício.
     */
    @Query("""
            SELECT p FROM Portfolio p
            JOIN FETCH p.user u
            WHERE p.competition.roundNumber BETWEEN :start AND :end
              AND p.competition.status IN :statuses
              AND u.role <> :adminRole
            ORDER BY p.competition.roundNumber ASC
            """)
    List<Portfolio> findForSeasonRanking(
            @Param("start") int start,
            @Param("end") int end,
            @Param("statuses") Collection<String> statuses,
            @Param("adminRole") String adminRole);

    /** Conta quantas competições um usuário participou. */
    long countByUserId(UUID userId);

    /**
     * Conta competições jogadas por vários usuários de uma vez (evita N+1 no
     * ranking global, que antes chamava {@link #countByUserId} por usuário da página).
     */
    @Query("SELECT p.user.id AS userId, COUNT(p) AS total FROM Portfolio p WHERE p.user.id IN :userIds GROUP BY p.user.id")
    List<UserPortfolioCount> countByUserIdIn(@Param("userIds") Collection<UUID> userIds);

    interface UserPortfolioCount {
        UUID getUserId();
        long getTotal();
    }
}
