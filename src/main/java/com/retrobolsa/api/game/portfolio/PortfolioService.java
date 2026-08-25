package com.retrobolsa.api.game.portfolio;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.HistoricalQuote;
import com.retrobolsa.api.game.asset.HistoricalQuoteRepository;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
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

    @Transactional
    public SubmitPortfolioResponseDto submit(UUID userId, SubmitPortfolioRequestDto request) {
        UUID competitionId = UUID.fromString(request.getCompetitionId());

        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new IllegalArgumentException("Rodada nao encontrada"));

        if (!"open".equals(competition.getStatus())) {
            throw new IllegalArgumentException("Esta rodada nao esta aberta para submissoes");
        }

        if (portfolioRepository.findByUserIdAndCompetitionId(userId, competitionId).isPresent()) {
            throw new IllegalArgumentException("Voce ja submeteu uma carteira para esta rodada");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        Set<UUID> competitionAssetIds = new HashSet<>();
        for (Asset a : competition.getAssets()) {
            competitionAssetIds.add(a.getId());
        }

        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<SimulationEngine.AllocationInput> simulationInputs = new ArrayList<>();
        List<AllocationData> allocationDataList = new ArrayList<>();

        for (SubmitPortfolioRequestDto.AllocationRequestDto alloc : request.getAllocations()) {
            UUID assetId = UUID.fromString(alloc.getAssetId());

            if (!competitionAssetIds.contains(assetId)) {
                throw new IllegalArgumentException("Ativo " + alloc.getAssetId() + " nao pertence a esta rodada");
            }

            if (alloc.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("O valor alocado deve ser positivo");
            }

            Asset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new IllegalArgumentException("Ativo nao encontrado: " + alloc.getAssetId()));

            List<HistoricalQuote> quotes = quoteRepository
                    .findAllByAssetIdAndDateBetweenOrderByDateAsc(assetId, LocalDate.of(competition.getStartYear() - 1, 12, 1), LocalDate.of(competition.getEndYear(), 12, 31));

            List<AssetSnapshot> snapshots = snapshotRepository
                    .findByAssetIdAndYearBetweenOrderByYearAsc(
                            assetId, competition.getStartYear(), competition.getEndYear());
            if (quotes.isEmpty() && snapshots.isEmpty()) {
                throw new IllegalStateException("Dados historicos indisponiveis para o ativo: " + asset.getAnonymousName());
            }

            totalAllocated = totalAllocated.add(alloc.getAmount());
            simulationInputs.add(new SimulationEngine.AllocationInput(assetId, alloc.getAmount(), quotes));
            allocationDataList.add(new AllocationData(asset, alloc.getAmount()));
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

        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .competition(competition)
                .build();
        portfolio = portfolioRepository.save(portfolio);

        List<Allocation> allocations = new ArrayList<>();
        for (AllocationData data : allocationDataList) {
            BigDecimal weight = data.amount.divide(competition.getBudget(), 4, RoundingMode.HALF_UP);
            allocations.add(Allocation.builder()
                    .portfolio(portfolio)
                    .asset(data.asset)
                    .amountInvested(data.amount)
                    .percentWeight(weight)
                    .build());
        }
        allocationRepository.saveAll(allocations);

        return SubmitPortfolioResponseDto.builder()
                .message("Carteira submetida com sucesso")
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    @Transactional(readOnly = true)
    public PortfolioResultDto getLastResult(UUID userId) {
        Portfolio portfolio = portfolioRepository.findTopByUserIdOrderBySubmittedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum portfolio encontrado"));

        Competition competition = portfolio.getCompetition();
        if (!"simulated".equals(competition.getStatus()) && !"revealed".equals(competition.getStatus())) {
            throw new IllegalArgumentException("O resultado ainda nao foi simulado");
        }

        List<SimulationEngine.AllocationInput> inputs = new ArrayList<>();
        for (Allocation alloc : portfolio.getAllocations()) {
            List<HistoricalQuote> quotes = quoteRepository
                    .findAllByAssetIdAndDateBetweenOrderByDateAsc(alloc.getAsset().getId(),
                            LocalDate.of(competition.getStartYear() - 1, 12, 1), LocalDate.of(competition.getEndYear(), 12, 31));
            inputs.add(new SimulationEngine.AllocationInput(alloc.getAsset().getId(), alloc.getAmountInvested(), quotes));
        }

        SimulationEngine.SimulationResult result = calculateResult(
                portfolio, competition, inputs);

        List<PortfolioResultDto.RevealedAssetDto> revealedAssets = new ArrayList<>();
        for (SimulationEngine.AssetFinalValue afv : result.assetFinalValues()) {
            Asset asset = assetRepository.findById(afv.assetId())
                    .orElseThrow(() -> new IllegalStateException("Ativo nao encontrado"));

            Allocation matchingAlloc = null;
            for (Allocation a : portfolio.getAllocations()) {
                if (a.getAsset().getId().equals(afv.assetId())) {
                    matchingAlloc = a;
                    break;
                }
            }

            boolean revealed = "revealed".equals(competition.getStatus());
            revealedAssets.add(PortfolioResultDto.RevealedAssetDto.builder()
                    .id(asset.getId().toString())
                    .anonymousName(asset.getAnonymousName())
                    .realName(revealed ? asset.getRealName() : null)
                    .ticker(revealed ? asset.getTicker() : null)
                    .type(asset.getType())
                    .sector(asset.getSector())
                    .bondType(asset.getBondType())
                    .amountInvested(matchingAlloc != null ? matchingAlloc.getAmountInvested() : BigDecimal.ZERO)
                    .finalValue(afv.finalValue())
                    .build());
        }

        return PortfolioResultDto.builder()
                .rank(portfolio.getRank() != null ? portfolio.getRank() : 0)
                .rentability(result.totalReturn())
                .annualReturn(result.annualReturn())
                .portfolioValue(result.finalValue())
                .chartData(result.chartData())
                .revealedAssets(revealedAssets)
                .period(competition.getStartYear() + "-" + competition.getEndYear())
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
        for (Portfolio portfolio : portfolios) {
            List<SimulationEngine.AllocationInput> inputs = new ArrayList<>();
            for (Allocation allocation : portfolio.getAllocations()) {
                List<HistoricalQuote> quotes = quoteRepository
                        .findAllByAssetIdAndDateBetweenOrderByDateAsc(
                                allocation.getAsset().getId(),
                                LocalDate.of(competition.getStartYear() - 1, 12, 1),
                                LocalDate.of(competition.getEndYear(), 12, 31));
                inputs.add(new SimulationEngine.AllocationInput(
                        allocation.getAsset().getId(), allocation.getAmountInvested(), quotes));
            }

            SimulationEngine.SimulationResult result = calculateResult(
                    portfolio, competition, inputs);
            portfolio.setTotalReturn(result.totalReturn());
            portfolio.setFinalValue(result.finalValue());
        }

        recalculateRanks(competition.getId());
        competition.setStatus("simulated");
        competitionRepository.save(competition);
    }

    private void recalculateRanks(UUID competitionId) {
        List<Portfolio> portfolios = portfolioRepository.findByCompetitionIdOrderByTotalReturnDesc(competitionId);
        for (int i = 0; i < portfolios.size(); i++) {
            portfolios.get(i).setRank(i + 1);
        }
        portfolioRepository.saveAll(portfolios);
    }

    private SimulationEngine.SimulationResult calculateResult(
            Portfolio portfolio, Competition competition, List<SimulationEngine.AllocationInput> inputs) {
        if (inputs.stream().allMatch(input -> !input.quotes().isEmpty())) {
            return simulationEngine.calculate(
                    competition.getBudget(), inputs, competition.getStartYear(), competition.getEndYear());
        }

        List<SimulationEngine.SnapshotAllocationInput> snapshotInputs = portfolio.getAllocations().stream()
                .map(allocation -> new SimulationEngine.SnapshotAllocationInput(
                        allocation.getAsset().getId(),
                        allocation.getAmountInvested(),
                        snapshotRepository.findByAssetIdAndYearBetweenOrderByYearAsc(
                                allocation.getAsset().getId(),
                                competition.getStartYear(),
                                competition.getEndYear())))
                .toList();
        return simulationEngine.calculateSnapshots(
                competition.getBudget(), snapshotInputs,
                competition.getStartYear(), competition.getEndYear());
    }

    private record AllocationData(Asset asset, BigDecimal amount) {}
}
