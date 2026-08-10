package com.retrobolsa.api.game.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByUserIdAndCompetitionId(UUID userId, UUID competitionId);

    Optional<Portfolio> findTopByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<Portfolio> findByCompetitionIdOrderByTotalReturnDesc(UUID competitionId);

    List<Portfolio> findByCompetitionIdOrderByRankAsc(UUID competitionId);

    List<Portfolio> findByCompetitionRoundNumberOrderByRankAsc(int roundNumber);
}
