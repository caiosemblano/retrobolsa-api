package com.retrobolsa.api.game.ranking;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.GlobalRankingResponseDto;
import com.retrobolsa.api.game.dto.RankingResponseDto;
import com.retrobolsa.api.game.dto.SeasonInfoDto;
import com.retrobolsa.api.game.dto.UserRankSummaryDto;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.game.portfolio.ScoringCalculator;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final List<String> FINISHED_STATUSES = List.of("simulated", "revealed");
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int DEFAULT_SEASON_SIZE = 4;
    /** Contas administrativas não são jogadores reais e não devem aparecer em nenhum ranking. */
    private static final String ADMIN_ROLE = "ADMIN";

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final CompetitionRepository competitionRepository;

    /** Quantas rodadas compõem uma temporada. */
    @Value("${retrobolsa.ranking.season-size:4}")
    private int seasonSize = DEFAULT_SEASON_SIZE;

    // -------------------------------------------------------------------------
    // Ranking Quinzenal / Rodada Ativa
    // -------------------------------------------------------------------------

    /**
     * Retorna o ranking da rodada ativa ou da última rodada finalizada.
     * <p>
     * Estratégia:
     * <ol>
     *   <li>Se existe uma rodada com status {@code "open"}, retorna os portfólios
     *       submetidos até agora (rank e resultados podem ser nulos pois a simulação
     *       ainda não ocorreu). O campo {@code roundStatus} do DTO sinaliza "open".</li>
     *   <li>Caso não exista rodada aberta, busca a última rodada com status
     *       {@code "simulated"} ou {@code "revealed"} e retorna os portfólios
     *       já com rank calculado.</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    public List<RankingResponseDto> getQuinzenalRanking() {
        // 1. Tenta encontrar rodada aberta
        Optional<Competition> activeOpt = competitionRepository.findByStatus("open");
        if (activeOpt.isPresent()) {
            Competition active = activeOpt.get();
            List<Portfolio> portfolios =
                    portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(active.getId());
            return portfolios.stream()
                    .map(p -> toRankingResponse(p, active.getStatus()))
                    .toList();
        }

        // 2. Fallback: última rodada finalizada
        return competitionRepository
                .findTopByStatusInOrderByRoundNumberDesc(FINISHED_STATUSES)
                .map(comp -> portfolioRepository
                        .findByCompetitionIdOrderByRankThenTieBreak(comp.getId())
                        .stream()
                        .map(p -> toRankingResponse(p, comp.getStatus()))
                        .toList())
                .orElse(List.of());
    }

    // -------------------------------------------------------------------------
    // Ranking de Rodada Específica
    // -------------------------------------------------------------------------

    /**
     * Retorna o ranking de uma rodada específica, identificada por {@code competitionId}
     * ou por {@code roundNumber}. Exatamente um dos dois parâmetros deve ser fornecido.
     *
     * @param competitionId UUID da competição (exclusivo com roundNumber)
     * @param roundNumber   número da rodada (exclusivo com competitionId)
     * @throws IllegalArgumentException se nenhum ou ambos os parâmetros forem fornecidos,
     *                                  ou se a competição não for encontrada
     */
    @Transactional(readOnly = true)
    public List<RankingResponseDto> getRanking(UUID competitionId, Integer roundNumber) {
        if (competitionId == null && roundNumber == null) {
            throw new IllegalArgumentException(
                    "Informe competitionId ou roundNumber para consultar o ranking da rodada");
        }
        if (competitionId != null && roundNumber != null) {
            throw new IllegalArgumentException("Use apenas um filtro de ranking por vez");
        }

        List<Portfolio> portfolios;
        String roundStatus;

        if (competitionId != null) {
            Competition comp = competitionRepository.findById(competitionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Competição não encontrada: " + competitionId));
            roundStatus = comp.getStatus();
            portfolios = portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(competitionId);
        } else {
            portfolios = portfolioRepository.findByRoundNumberOrderByRankThenTieBreak(roundNumber);
            roundStatus = portfolios.isEmpty() ? "unknown"
                    : portfolios.get(0).getCompetition().getStatus();
        }

        return portfolios.stream()
                .map(p -> toRankingResponse(p, roundStatus))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Ranking Global
    // -------------------------------------------------------------------------

    /**
     * Retorna o ranking global paginado, ordenado por {@code totalScore} DESC e
     * {@code username} ASC como critério de desempate.
     *
     * @param limit número máximo de entradas por página (padrão: 50, máximo: 200)
     * @param page  número da página, base 0 (padrão: 0)
     */
    @Transactional(readOnly = true)
    public List<GlobalRankingResponseDto> getGlobalRanking(Integer limit, Integer page) {
        int pageSize = (limit == null || limit <= 0) ? DEFAULT_PAGE_SIZE : Math.min(limit, 200);
        int pageNumber = (page == null || page < 0) ? 0 : page;

        List<User> users = userRepository.findAllByRoleNotOrderByTotalScoreDescUsernameAsc(
                ADMIN_ROLE, PageRequest.of(pageNumber, pageSize));

        // Offset para numerar o rank corretamente em páginas subsequentes
        int rankOffset = pageNumber * pageSize;

        List<UUID> userIds = users.stream().map(User::getId).toList();
        Map<UUID, Long> competitionsPlayedByUser = userIds.isEmpty() ? Map.of()
                : portfolioRepository.countByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                PortfolioRepository.UserPortfolioCount::getUserId,
                                PortfolioRepository.UserPortfolioCount::getTotal));

        List<GlobalRankingResponseDto> ranking = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            int competitionsPlayed = competitionsPlayedByUser.getOrDefault(user.getId(), 0L).intValue();
            ranking.add(GlobalRankingResponseDto.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .rank(rankOffset + i + 1)
                    .totalScore(user.getTotalScore())
                    .competitionsPlayed(competitionsPlayed)
                    .build());
        }
        return ranking;
    }

    // -------------------------------------------------------------------------
    // Ranking de Temporada
    // -------------------------------------------------------------------------

    /**
     * Retorna os limites da temporada atual. Uma temporada agrupa
     * {@code retrobolsa.ranking.season-size} rodadas consecutivas: com o padrão de 4,
     * as rodadas 1-4 formam a temporada 1, as rodadas 5-8 a temporada 2, e assim por
     * diante. A temporada atual é a que contém a rodada mais recente cadastrada.
     */
    @Transactional(readOnly = true)
    public SeasonInfoDto getCurrentSeasonInfo() {
        int size = seasonSize > 0 ? seasonSize : DEFAULT_SEASON_SIZE;
        int latestRound = competitionRepository.findTopByOrderByRoundNumberDesc()
                .map(Competition::getRoundNumber)
                .orElse(1);
        int seasonNumber = ((Math.max(latestRound, 1) - 1) / size) + 1;
        return SeasonInfoDto.builder()
                .seasonNumber(seasonNumber)
                .roundStart((seasonNumber - 1) * size + 1)
                .roundEnd(seasonNumber * size)
                .build();
    }

    /**
     * Retorna o ranking da temporada atual: apenas as rodadas já simuladas dentro da
     * faixa da temporada contam, e a pontuação é acumulada do zero (diferente do
     * ranking global, que é vitalício). A soma reproduz a mesma regra do score
     * vitalício — pontos por rodada com piso em zero — só que reiniciada a cada
     * temporada.
     *
     * @param limit número máximo de entradas por página (padrão: 50, máximo: 200)
     * @param page  número da página, base 0 (padrão: 0)
     */
    @Transactional(readOnly = true)
    public List<GlobalRankingResponseDto> getSeasonRanking(Integer limit, Integer page) {
        SeasonInfoDto season = getCurrentSeasonInfo();
        List<Portfolio> portfolios = portfolioRepository.findForSeasonRanking(
                season.getRoundStart(), season.getRoundEnd(), FINISHED_STATUSES, ADMIN_ROLE);

        Map<UUID, SeasonScore> scoresByUser = new LinkedHashMap<>();
        for (Portfolio portfolio : portfolios) {
            User user = portfolio.getUser();
            scoresByUser.computeIfAbsent(user.getId(), id -> new SeasonScore(user))
                    .add(portfolio.getTotalReturn());
        }

        List<SeasonScore> ordered = scoresByUser.values().stream()
                .sorted(Comparator.comparingInt((SeasonScore s) -> s.score).reversed()
                        .thenComparing(s -> s.user.getUsername()))
                .toList();

        int pageSize = (limit == null || limit <= 0) ? DEFAULT_PAGE_SIZE : Math.min(limit, 200);
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int from = Math.min(pageNumber * pageSize, ordered.size());
        int to = Math.min(from + pageSize, ordered.size());

        List<GlobalRankingResponseDto> ranking = new ArrayList<>();
        for (int i = from; i < to; i++) {
            SeasonScore entry = ordered.get(i);
            ranking.add(GlobalRankingResponseDto.builder()
                    .userId(entry.user.getId())
                    .username(entry.user.getUsername())
                    .rank(i + 1)
                    .totalScore(entry.score)
                    .competitionsPlayed(entry.roundsPlayed)
                    .build());
        }
        return ranking;
    }

    /** Acumulador de pontos de um usuário dentro da temporada. */
    private static final class SeasonScore {
        private final User user;
        private int score;
        private int roundsPlayed;

        private SeasonScore(User user) {
            this.user = user;
        }

        private void add(BigDecimal totalReturn) {
            score = Math.max(0, score + ScoringCalculator.pointsFromReturn(totalReturn));
            roundsPlayed++;
        }
    }

    // -------------------------------------------------------------------------
    // Posição do Usuário Autenticado
    // -------------------------------------------------------------------------

    /**
     * Calcula e retorna a posição do usuário identificado pelo e-mail no ranking global
     * e na rodada ativa/mais recente.
     *
     * @param email e-mail do usuário autenticado (subject do JWT)
     * @throws IllegalArgumentException se o usuário não for encontrado
     */
    @Transactional(readOnly = true)
    public UserRankSummaryDto getUserRankSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + email));

        // Ranking global: posição calculada via contagem indexada, sem carregar a tabela toda,
        // e excluindo contas administrativas (não são jogadores reais).
        long usersAhead = userRepository.countByTotalScoreGreaterThanAndRoleNot(user.getTotalScore(), ADMIN_ROLE)
                + userRepository.countByTotalScoreAndUsernameLessThanAndRoleNot(
                        user.getTotalScore(), user.getUsername(), ADMIN_ROLE);
        int globalRank = (int) usersAhead + 1;
        long totalGlobalPlayers = userRepository.countByRoleNot(ADMIN_ROLE);
        int competitionsPlayed = (int) portfolioRepository.countByUserId(user.getId());

        // Rodada ativa: tenta open primeiro, depois última finalizada
        Integer activeRoundRank = null;
        Integer activeRoundNumber = null;
        String activeRoundStatus = null;

        Optional<Competition> activeCompetition = competitionRepository.findByStatus("open");
        if (activeCompetition.isEmpty()) {
            activeCompetition = competitionRepository
                    .findTopByStatusInOrderByRoundNumberDesc(FINISHED_STATUSES);
        }

        if (activeCompetition.isPresent()) {
            Competition comp = activeCompetition.get();
            activeRoundNumber = comp.getRoundNumber();
            activeRoundStatus = comp.getStatus();

            Optional<Portfolio> userPortfolio =
                    portfolioRepository.findByUserIdAndCompetitionId(user.getId(), comp.getId());
            if (userPortfolio.isPresent()) {
                Portfolio p = userPortfolio.get();
                if (p.getRank() != null) {
                    activeRoundRank = p.getRank();
                } else {
                    // Rodada ainda aberta: calcula posição dinâmica
                    List<Portfolio> all = portfolioRepository
                            .findByCompetitionIdOrderByRankThenTieBreak(comp.getId());
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).getId().equals(p.getId())) {
                            activeRoundRank = i + 1;
                            break;
                        }
                    }
                }
            }
        }

        return UserRankSummaryDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .globalRank(globalRank)
                .totalGlobalPlayers(totalGlobalPlayers)
                .totalScore(user.getTotalScore())
                .competitionsPlayed(competitionsPlayed)
                .activeRoundRank(activeRoundRank)
                .activeRoundNumber(activeRoundNumber)
                .activeRoundStatus(activeRoundStatus)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers Privados
    // -------------------------------------------------------------------------

    private RankingResponseDto toRankingResponse(Portfolio portfolio, String roundStatus) {
        return RankingResponseDto.builder()
                .userId(portfolio.getUser().getId())
                .username(portfolio.getUser().getUsername())
                .rank(portfolio.getRank() != null ? portfolio.getRank() : 0)
                .totalReturn(portfolio.getTotalReturn())
                .finalValue(portfolio.getFinalValue())
                .roundNumber(portfolio.getCompetition().getRoundNumber())
                .submittedAt(portfolio.getSubmittedAt())
                .roundStatus(roundStatus)
                .build();
    }
}
