package com.retrobolsa.api.game.portfolio;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.HistoricalQuote;
import com.retrobolsa.api.game.asset.HistoricalQuoteRepository;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.debrief.DebriefAdvisor;
import com.retrobolsa.api.game.debrief.DebriefService;
import com.retrobolsa.api.game.dto.CurrentPortfolioDto;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
import com.retrobolsa.api.game.progress.ProgressService;
import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.game.progress.XpSource;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import com.retrobolsa.api.game.dto.SubmitPortfolioResponseDto;
import com.retrobolsa.api.game.simulation.SimulationEngine;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AllocationRepository allocationRepository;
    private final CompetitionRepository competitionRepository;
    private final AssetRepository assetRepository;
    private final HistoricalQuoteRepository quoteRepository;
    private final AssetSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final SimulationEngine simulationEngine;
    private final AchievementService achievementService;
    private final DebriefService debriefService;
    private final ProgressService progressService;

    /** Uma posição da carteira: o ativo e quanto foi posto nele. */
    public record Holding(Asset asset, BigDecimal amount) {}

    /** Posições já conferidas contra a rodada, com o aviso de dinheiro parado, se houver. */
    public record ValidatedHoldings(List<Holding> holdings, List<String> warnings) {}

    @Transactional
    public SubmitPortfolioResponseDto submit(UUID userId, SubmitPortfolioRequestDto request) {
        Competition competition = openCompetition(request.getCompetitionId());

        if (portfolioRepository.findByUserIdAndCompetitionId(userId, competition.getId()).isPresent()) {
            throw new IllegalArgumentException("Voce ja submeteu uma carteira para esta rodada");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        ValidatedHoldings validated = validate(competition, request.getAllocations());

        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .competition(competition)
                .build();
        portfolio = portfolioRepository.save(portfolio);

        List<Allocation> allocations = allocationsFor(portfolio, competition, validated.holdings());
        allocationRepository.saveAll(allocations);

        achievementService.evaluateOnSubmit(user, competition.getBudget(), allocations,
                portfolioRepository.countByUserId(userId));
        progressService.reward(user, XpSource.PORTFOLIO, competition.getId().toString(), XpService.PORTFOLIO_XP);

        return SubmitPortfolioResponseDto.builder()
                .message("Carteira submetida com sucesso")
                .warnings(validated.warnings().isEmpty() ? null : validated.warnings())
                .build();
    }

    /**
     * Troca as alocações de uma carteira já enviada, enquanto a rodada está aberta.
     * As conquistas de montagem são reavaliadas (a nova carteira pode merecer outras);
     * o XP de envio não se repete, porque ele é dado uma vez por rodada.
     */
    @Transactional
    public SubmitPortfolioResponseDto update(UUID userId, SubmitPortfolioRequestDto request) {
        Competition competition = openCompetition(request.getCompetitionId());

        Portfolio portfolio = portfolioRepository.findByUserIdAndCompetitionId(userId, competition.getId())
                .orElseThrow(() -> new IllegalArgumentException("Você ainda não enviou uma carteira para esta rodada"));

        ValidatedHoldings validated = validate(competition, request.getAllocations());

        // Apaga as antigas antes de gravar as novas, na mesma transação.
        portfolio.getAllocations().clear();
        portfolioRepository.flush();
        List<Allocation> allocations = allocationsFor(portfolio, competition, validated.holdings());
        portfolio.getAllocations().addAll(allocations);
        // No desempate do ranking vale quem enviou primeiro: editar conta como enviar de novo.
        portfolio.setSubmittedAt(LocalDateTime.now());
        portfolioRepository.save(portfolio);

        achievementService.evaluateOnSubmit(portfolio.getUser(), competition.getBudget(), allocations,
                portfolioRepository.countByUserId(userId));

        return SubmitPortfolioResponseDto.builder()
                .message("Carteira atualizada com sucesso")
                .warnings(validated.warnings().isEmpty() ? null : validated.warnings())
                .build();
    }

    /** A carteira do jogador na rodada aberta; vazia se não há rodada aberta ou se ele ainda não enviou. */
    @Transactional(readOnly = true)
    public Optional<CurrentPortfolioDto> current(UUID userId) {
        return competitionRepository.findByStatus("open")
                .flatMap(competition -> portfolioRepository.findByUserIdAndCompetitionId(userId, competition.getId()))
                .map(portfolio -> CurrentPortfolioDto.builder()
                        .competitionId(portfolio.getCompetition().getId().toString())
                        .allocations(portfolio.getAllocations().stream()
                                .map(allocation -> CurrentPortfolioDto.Position.builder()
                                        .assetId(allocation.getAsset().getId().toString())
                                        .amount(allocation.getAmountInvested())
                                        .build())
                                .toList())
                        .build());
    }

    private Competition openCompetition(String competitionId) {
        Competition competition = competitionRepository.findById(UUID.fromString(competitionId))
                .orElseThrow(() -> new IllegalArgumentException("Rodada nao encontrada"));

        if (!"open".equals(competition.getStatus())) {
            throw new IllegalArgumentException("Esta rodada nao esta aberta para submissoes");
        }
        return competition;
    }

    /**
     * Confere as alocações pedidas contra a rodada: ativos dela, sem repetidos,
     * valores positivos, dados históricos disponíveis e total dentro do orçamento.
     * Usada no envio, na edição e no treino.
     */
    @Transactional(readOnly = true)
    public ValidatedHoldings validate(Competition competition,
                                      List<SubmitPortfolioRequestDto.AllocationRequestDto> requests) {
        Set<UUID> competitionAssetIds = new HashSet<>();
        for (Asset a : competition.getAssets()) {
            competitionAssetIds.add(a.getId());
        }

        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<Holding> holdings = new ArrayList<>();
        Set<UUID> seenAssetIds = new HashSet<>();

        for (SubmitPortfolioRequestDto.AllocationRequestDto alloc : requests) {
            UUID assetId = UUID.fromString(alloc.getAssetId());

            if (!seenAssetIds.add(assetId)) {
                throw new IllegalArgumentException("Ativo " + alloc.getAssetId() + " foi informado mais de uma vez na submissao");
            }

            if (!competitionAssetIds.contains(assetId)) {
                throw new IllegalArgumentException("Ativo " + alloc.getAssetId() + " nao pertence a esta rodada");
            }

            if (alloc.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("O valor alocado deve ser positivo");
            }

            Asset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new IllegalArgumentException("Ativo nao encontrado: " + alloc.getAssetId()));

            if (quotesFor(assetId, competition).isEmpty() && snapshotsFor(assetId, competition).isEmpty()) {
                throw new IllegalStateException("Dados historicos indisponiveis para o ativo: " + asset.getAnonymousName());
            }

            totalAllocated = totalAllocated.add(alloc.getAmount());
            holdings.add(new Holding(asset, alloc.getAmount()));
        }

        if (totalAllocated.compareTo(competition.getBudget()) > 0) {
            throw new IllegalArgumentException("O total alocado (R$ " + totalAllocated.setScale(2, RoundingMode.HALF_UP) +
                    ") excede o orcamento da rodada (R$ " + competition.getBudget().setScale(2, RoundingMode.HALF_UP) + ")");
        }

        List<String> warnings = new ArrayList<>();
        if (totalAllocated.compareTo(competition.getBudget()) < 0) {
            BigDecimal remaining = competition.getBudget().subtract(totalAllocated).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct = totalAllocated.divide(competition.getBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            warnings.add("Voce alocou apenas " + pct + "% do orcamento. R$ " + remaining +
                    " ficaram parados em caixa com rentabilidade 0%.");
        }
        return new ValidatedHoldings(holdings, warnings);
    }

    private List<Allocation> allocationsFor(Portfolio portfolio, Competition competition, List<Holding> holdings) {
        List<Allocation> allocations = new ArrayList<>();
        for (Holding holding : holdings) {
            BigDecimal weight = holding.amount().divide(competition.getBudget(), 4, RoundingMode.HALF_UP);
            allocations.add(Allocation.builder()
                    .portfolio(portfolio)
                    .asset(holding.asset())
                    .amountInvested(holding.amount())
                    .percentWeight(weight)
                    .build());
        }
        return allocations;
    }

    @Transactional(readOnly = true)
    public PortfolioResultDto getLastResult(UUID userId) {
        Portfolio portfolio = portfolioRepository.findTopByUserIdOrderBySubmittedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum portfolio encontrado"));

        Competition competition = portfolio.getCompetition();
        if (!"simulated".equals(competition.getStatus()) && !"revealed".equals(competition.getStatus())) {
            throw new IllegalArgumentException("O resultado ainda nao foi simulado");
        }

        return result(competition, holdingsOf(portfolio), portfolio.getRank() != null ? portfolio.getRank() : 0);
    }

    /**
     * Simula as posições no período da rodada e monta o resultado completo:
     * referências, contribuição de cada ativo, dicas e, depois da revelação,
     * os nomes reais e a história do período.
     */
    @Transactional(readOnly = true)
    public PortfolioResultDto result(Competition competition, List<Holding> holdings, int rank) {
        List<SimulationEngine.AllocationInput> inputs = new ArrayList<>();
        for (Holding holding : holdings) {
            inputs.add(new SimulationEngine.AllocationInput(
                    holding.asset().getId(), holding.amount(), quotesFor(holding.asset().getId(), competition)));
        }

        SimulationEngine.SimulationResult result = calculateResult(holdings, competition, inputs);

        boolean revealed = "revealed".equals(competition.getStatus());
        List<PortfolioResultDto.RevealedAssetDto> revealedAssets = new ArrayList<>();
        List<DebriefAdvisor.Position> positions = new ArrayList<>();
        for (SimulationEngine.AssetFinalValue afv : result.assetFinalValues()) {
            Asset asset = assetRepository.findById(afv.assetId())
                    .orElseThrow(() -> new IllegalStateException("Ativo nao encontrado"));

            BigDecimal invested = holdings.stream()
                    .filter(h -> h.asset().getId().equals(afv.assetId()))
                    .map(Holding::amount)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            positions.add(new DebriefAdvisor.Position(asset.getAnonymousName(), asset.getType(), invested, afv.finalValue()));
            revealedAssets.add(PortfolioResultDto.RevealedAssetDto.builder()
                    .id(asset.getId().toString())
                    .anonymousName(asset.getAnonymousName())
                    .realName(revealed ? asset.getRealName() : null)
                    .ticker(revealed ? asset.getTicker() : null)
                    .type(asset.getType())
                    .sector(asset.getSector())
                    .bondType(asset.getBondType())
                    .amountInvested(invested)
                    .finalValue(afv.finalValue())
                    .returnPct(invested.signum() == 0 ? BigDecimal.ZERO
                            : afv.finalValue().subtract(invested).divide(invested, 6, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    // Em pontos do orçamento: somando as contribuições (e o caixa parado, que é zero) dá a rentabilidade.
                    .contribution(afv.finalValue().subtract(invested)
                            .divide(competition.getBudget(), 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .revealNote(revealed ? asset.getRevealNote() : null)
                    .build());
        }

        DebriefService.Debrief debrief = debriefService.build(competition, positions, result.totalReturn(), revealed);

        return PortfolioResultDto.builder()
                .rank(rank)
                .rentability(result.totalReturn())
                .annualReturn(result.annualReturn())
                .portfolioValue(result.finalValue())
                .chartData(result.chartData())
                .revealedAssets(revealedAssets)
                .period(competition.getStartYear() + "-" + competition.getEndYear())
                .benchmarks(debrief.benchmarks())
                .roundStats(debrief.roundStats())
                .debrief(debrief.debrief())
                .tips(debrief.tips())
                .build();
    }

    @Transactional
    public void simulateCompetition(Competition competition) {
        if (!"closed".equals(competition.getStatus())) {
            throw new IllegalArgumentException("A rodada precisa estar fechada para ser simulada");
        }

        competition.setStatus("simulating");
        competitionRepository.save(competition);

        List<Portfolio> portfolios = portfolioRepository.findByCompetitionIdOrderByTotalReturnDesc(competition.getId());

        Set<UUID> allAssetIds = new HashSet<>();
        for (Portfolio portfolio : portfolios) {
            for (Allocation allocation : portfolio.getAllocations()) {
                allAssetIds.add(allocation.getAsset().getId());
            }
        }
        Map<UUID, List<HistoricalQuote>> quotesByAsset = new HashMap<>();
        if (!allAssetIds.isEmpty()) {
            List<HistoricalQuote> allQuotes = quoteRepository.findAllByAssetIdInAndDateBetweenOrderByAssetIdAscDateAsc(
                    allAssetIds,
                    LocalDate.of(competition.getStartYear() - 1, 12, 1),
                    LocalDate.of(competition.getEndYear(), 12, 31));
            for (HistoricalQuote quote : allQuotes) {
                quotesByAsset.computeIfAbsent(quote.getAsset().getId(), key -> new ArrayList<>()).add(quote);
            }
        }

        for (Portfolio portfolio : portfolios) {
            List<SimulationEngine.AllocationInput> inputs = new ArrayList<>();
            for (Allocation allocation : portfolio.getAllocations()) {
                UUID assetId = allocation.getAsset().getId();
                List<HistoricalQuote> quotes = quotesByAsset.getOrDefault(assetId, List.of());
                inputs.add(new SimulationEngine.AllocationInput(
                        assetId, allocation.getAmountInvested(), quotes));
            }

            SimulationEngine.SimulationResult result = calculateResult(
                    holdingsOf(portfolio), competition, inputs);
            portfolio.setTotalReturn(result.totalReturn());
            portfolio.setFinalValue(result.finalValue());
        }

        recalculateRanks(competition);
        competition.setStatus("simulated");
        competitionRepository.save(competition);
    }

    private void recalculateRanks(Competition competition) {
        List<Portfolio> portfolios = portfolioRepository.findByCompetitionIdOrderByTotalReturnDesc(competition.getId());
        DebriefService.References references = debriefService.references(competition);
        for (int i = 0; i < portfolios.size(); i++) {
            Portfolio portfolio = portfolios.get(i);
            portfolio.setRank(i + 1);

            User user = portfolio.getUser();
            BigDecimal totalReturn = portfolio.getTotalReturn();
            if (totalReturn != null) {
                int pointsEarned = ScoringCalculator.pointsFromReturn(totalReturn);
                int newScore = Math.max(0, user.getTotalScore() + pointsEarned);
                user.setTotalScore(newScore);
                userRepository.save(user);
            }
            achievementService.evaluateOnRoundResult(user, i + 1, totalReturn, portfolios.size(),
                    references.cdi(), references.ipca());
            // Acima (e não igual) do CDI, como na conquista "Bateu o CDI".
            if (totalReturn != null && references.cdi() != null && totalReturn.compareTo(references.cdi()) > 0) {
                progressService.reward(user, XpSource.BEAT_CDI, competition.getId().toString(), XpService.BEAT_CDI_XP);
            }
        }
        portfolioRepository.saveAll(portfolios);
    }

    private List<Holding> holdingsOf(Portfolio portfolio) {
        return portfolio.getAllocations().stream()
                .map(allocation -> new Holding(allocation.getAsset(), allocation.getAmountInvested()))
                .toList();
    }

    private List<HistoricalQuote> quotesFor(UUID assetId, Competition competition) {
        return quoteRepository.findAllByAssetIdAndDateBetweenOrderByDateAsc(assetId,
                LocalDate.of(competition.getStartYear() - 1, 12, 1), LocalDate.of(competition.getEndYear(), 12, 31));
    }

    private List<AssetSnapshot> snapshotsFor(UUID assetId, Competition competition) {
        return snapshotRepository.findByAssetIdAndYearBetweenOrderByYearAsc(
                assetId, competition.getStartYear(), competition.getEndYear());
    }

    private SimulationEngine.SimulationResult calculateResult(
            List<Holding> holdings, Competition competition, List<SimulationEngine.AllocationInput> inputs) {
        if (inputs.stream().allMatch(input -> !input.quotes().isEmpty())) {
            return simulationEngine.calculate(
                    competition.getBudget(), inputs, competition.getStartYear(), competition.getEndYear());
        }

        List<SimulationEngine.SnapshotAllocationInput> snapshotInputs = holdings.stream()
                .map(holding -> new SimulationEngine.SnapshotAllocationInput(
                        holding.asset().getId(),
                        holding.amount(),
                        snapshotsFor(holding.asset().getId(), competition)))
                .toList();
        return simulationEngine.calculateSnapshots(
                competition.getBudget(), snapshotInputs,
                competition.getStartYear(), competition.getEndYear());
    }
}
