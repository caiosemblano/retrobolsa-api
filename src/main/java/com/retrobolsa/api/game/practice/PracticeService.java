package com.retrobolsa.api.game.practice;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.competition.CompetitionService;
import com.retrobolsa.api.game.dto.CompetitionResponseDto;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
import com.retrobolsa.api.game.dto.PracticeRoundDto;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import com.retrobolsa.api.game.progress.ProgressService;
import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.game.progress.XpSource;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Modo treino: o jogador remonta a carteira de uma rodada que já acabou e vê na
 * hora o que teria acontecido. Só rodadas reveladas, para não entregar a resposta
 * de uma rodada em andamento. Nada daqui entra em carteiras, ranking, pontos ou
 * conquistas de jogo; o XP vem uma vez por rodada treinada.
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private static final String REVEALED = "revealed";

    private final CompetitionRepository competitionRepository;
    private final CompetitionService competitionService;
    private final PortfolioService portfolioService;
    private final PracticeRunRepository practiceRunRepository;
    private final UserRepository userRepository;
    private final ProgressService progressService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PracticeRoundDto> rounds(UUID userId) {
        Map<UUID, PracticeRunRepository.Summary> summaries = practiceRunRepository.summarize(userId).stream()
                .collect(Collectors.toMap(PracticeRunRepository.Summary::getCompetitionId, Function.identity()));

        return competitionRepository.findAllByOrderByRoundNumberAsc().stream()
                .filter(c -> REVEALED.equals(c.getStatus()))
                .map(c -> {
                    PracticeRunRepository.Summary summary = summaries.get(c.getId());
                    return PracticeRoundDto.builder()
                            .id(c.getId().toString())
                            .round(c.getRoundNumber())
                            .scenarioTitle(c.getScenarioTitle())
                            .startYear(c.getStartYear())
                            .endYear(c.getEndYear())
                            .assetCount(c.getAssets().size())
                            .runs(summary != null ? summary.getRuns() : 0)
                            .bestReturn(summary != null ? summary.getBestReturn() : null)
                            .build();
                })
                .toList();
    }

    /** Os dados de montagem, como na rodada de verdade: ativos anônimos e o cenário do ano inicial. */
    @Transactional(readOnly = true)
    public CompetitionResponseDto round(UUID competitionId) {
        return competitionService.buildDto(revealed(competitionId));
    }

    @Transactional
    public PortfolioResultDto practice(UUID userId, UUID competitionId,
                                       List<SubmitPortfolioRequestDto.AllocationRequestDto> allocations) {
        Competition competition = revealed(competitionId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        PortfolioService.ValidatedHoldings validated = portfolioService.validate(competition, allocations);
        // Posição 0: o treino não entra no ranking. As estatísticas são as da rodada de verdade.
        PortfolioResultDto result = portfolioService.result(competition, validated.holdings(), 0);

        practiceRunRepository.save(PracticeRun.builder()
                .userId(userId)
                .competitionId(competitionId)
                .totalReturn(result.getRentability())
                .finalValue(result.getPortfolioValue())
                .createdAt(LocalDateTime.now(clock))
                .build());
        progressService.reward(user, XpSource.PRACTICE, competitionId.toString(), XpService.PRACTICE_XP);
        return result;
    }

    private Competition revealed(UUID competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new IllegalArgumentException("Rodada nao encontrada"));
        if (!REVEALED.equals(competition.getStatus())) {
            throw new IllegalArgumentException("Só dá para treinar em rodadas que já terminaram e foram reveladas");
        }
        return competition;
    }
}
