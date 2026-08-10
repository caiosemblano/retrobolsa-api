package com.retrobolsa.api.game.ranking;

import com.retrobolsa.api.game.dto.GlobalRankingResponseDto;
import com.retrobolsa.api.game.dto.RankingResponseDto;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RankingResponseDto> getRanking(UUID competitionId, Integer roundNumber) {
        if (competitionId == null && roundNumber == null) {
            throw new IllegalArgumentException("Informe competitionId ou roundNumber para consultar o ranking da rodada");
        }
        if (competitionId != null && roundNumber != null) {
            throw new IllegalArgumentException("Use apenas um filtro de ranking por vez");
        }

        List<Portfolio> portfolios = competitionId != null
                ? portfolioRepository.findByCompetitionIdOrderByRankAsc(competitionId)
                : portfolioRepository.findByCompetitionRoundNumberOrderByRankAsc(roundNumber);

        return portfolios.stream()
                .map(this::toRankingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GlobalRankingResponseDto> getGlobalRanking() {
        List<User> users = userRepository.findAllByOrderByTotalScoreDescUsernameAsc();
        List<GlobalRankingResponseDto> ranking = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            ranking.add(GlobalRankingResponseDto.builder()
                    .username(user.getUsername())
                    .rank(i + 1)
                    .totalScore(user.getTotalScore())
                    .build());
        }

        return ranking;
    }

    private RankingResponseDto toRankingResponse(Portfolio portfolio) {
        return RankingResponseDto.builder()
                .username(portfolio.getUser().getUsername())
                .rank(portfolio.getRank() != null ? portfolio.getRank() : 0)
                .totalReturn(portfolio.getTotalReturn())
                .finalValue(portfolio.getFinalValue())
                .roundNumber(portfolio.getCompetition().getRoundNumber())
                .build();
    }
}
