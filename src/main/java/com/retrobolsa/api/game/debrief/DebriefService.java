package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.asset.HistoricalQuote;
import com.retrobolsa.api.game.asset.HistoricalQuoteRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.dto.BenchmarkDto;
import com.retrobolsa.api.game.dto.RoundStatsDto;
import com.retrobolsa.api.game.dto.TipDto;
import com.retrobolsa.api.game.macro.MacroIndicator;
import com.retrobolsa.api.game.macro.MacroIndicatorRepository;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.game.simulation.SimulationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * O que o resultado da rodada ensina além do placar: comparação com CDI,
 * poupança, bolsa e inflação, como a rodada foi para todos, o que aconteceu
 * no período e as dicas tiradas da carteira do jogador.
 */
@Service
@RequiredArgsConstructor
public class DebriefService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final MacroIndicatorRepository macroIndicatorRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetSnapshotRepository snapshotRepository;
    private final HistoricalQuoteRepository quoteRepository;
    private final SimulationEngine simulationEngine;
    private final BenchmarkCalculator benchmarkCalculator;
    private final DebriefAdvisor debriefAdvisor;

    public record Debrief(List<BenchmarkDto> benchmarks, RoundStatsDto roundStats, String debrief, List<TipDto> tips) {}

    @Transactional(readOnly = true)
    public Debrief build(Competition competition, List<DebriefAdvisor.Position> positions,
                         BigDecimal totalReturn, boolean revealed) {
        int start = competition.getStartYear();
        int end = competition.getEndYear();
        List<MacroIndicator> macro = macroIndicatorRepository.findAllByYearBetweenOrderByYearAsc(start - 1, end - 1);
        List<BenchmarkDto> benchmarks = benchmarkCalculator.calculate(macro, competition.getBudget(), start, end);

        BigDecimal selicAtStart = macro.stream()
                .filter(m -> m.getYear() == start - 1)
                .findFirst()
                .map(m -> m.getSelicMeta() != null ? m.getSelicMeta() : m.getSelic())
                .orElse(null);
        List<TipDto> tips = debriefAdvisor.advise(new DebriefAdvisor.Input(
                competition.getBudget(), positions, totalReturn,
                benchmarkReturn(benchmarks, "CDI"), benchmarkReturn(benchmarks, "IPCA"), selicAtStart));

        return new Debrief(
                benchmarks,
                roundStats(competition, revealed),
                // O texto conta o que aconteceu com empresas pelo nome: só depois da revelação.
                revealed ? competition.getDebrief() : null,
                tips);
    }

    /** CDI e inflação acumulados no período da rodada (em %; nulos sem dados), para as recompensas do resultado. */
    @Transactional(readOnly = true)
    public References references(Competition competition) {
        int start = competition.getStartYear();
        int end = competition.getEndYear();
        List<BenchmarkDto> benchmarks = benchmarkCalculator.calculate(
                macroIndicatorRepository.findAllByYearBetweenOrderByYearAsc(start, end - 1),
                competition.getBudget(), start, end);
        return new References(benchmarkReturn(benchmarks, "CDI"), benchmarkReturn(benchmarks, "IPCA"));
    }

    public record References(BigDecimal cdi, BigDecimal ipca) {}

    private BigDecimal benchmarkReturn(List<BenchmarkDto> benchmarks, String code) {
        return benchmarks.stream().filter(b -> b.getCode().equals(code))
                .map(BenchmarkDto::getTotalReturn).findFirst().orElse(null);
    }

    private RoundStatsDto roundStats(Competition competition, boolean revealed) {
        // Mesmo critério dos rankings: carteiras de ADMIN não entram.
        List<BigDecimal> returns = portfolioRepository.findByCompetitionIdOrderByTotalReturnDesc(competition.getId())
                .stream()
                .filter(p -> p.getTotalReturn() != null && !ADMIN_ROLE.equals(p.getUser().getRole()))
                .map(Portfolio::getTotalReturn)
                .sorted()
                .toList();

        return RoundStatsDto.builder()
                .participants(returns.size())
                .medianReturn(median(returns))
                .bestAsset(bestAsset(competition, revealed).orElse(null))
                .build();
    }

    static BigDecimal median(List<BigDecimal> sorted) {
        if (sorted.isEmpty()) return null;
        int meio = sorted.size() / 2;
        BigDecimal mediana = sorted.size() % 2 == 1
                ? sorted.get(meio)
                : sorted.get(meio - 1).add(sorted.get(meio)).divide(BigDecimal.valueOf(2), MC);
        return mediana.setScale(2, RoundingMode.HALF_UP);
    }

    /** Simula cada ativo sozinho, com o orçamento inteiro, pelo mesmo caminho que a carteira usa no motor. */
    private Optional<RoundStatsDto.BestAssetDto> bestAsset(Competition competition, boolean revealed) {
        record Candidato(Asset ativo, BigDecimal retorno) {}
        return competition.getAssets().stream()
                .map(asset -> new Candidato(asset, soloReturn(asset, competition)))
                .max(Comparator.comparing(Candidato::retorno))
                .map(melhor -> RoundStatsDto.BestAssetDto.builder()
                        .anonymousName(melhor.ativo().getAnonymousName())
                        .realName(revealed ? melhor.ativo().getRealName() : null)
                        .returnPct(melhor.retorno())
                        .build());
    }

    private BigDecimal soloReturn(Asset asset, Competition competition) {
        BigDecimal budget = competition.getBudget();
        List<HistoricalQuote> quotes = quoteRepository.findAllByAssetIdAndDateBetweenOrderByDateAsc(
                asset.getId(),
                LocalDate.of(competition.getStartYear() - 1, 12, 1),
                LocalDate.of(competition.getEndYear(), 12, 31));
        SimulationEngine.SimulationResult result = quotes.isEmpty()
                ? simulationEngine.calculateSnapshots(budget,
                        List.of(new SimulationEngine.SnapshotAllocationInput(asset.getId(), budget,
                                snapshotRepository.findByAssetIdAndYearBetweenOrderByYearAsc(
                                        asset.getId(), competition.getStartYear(), competition.getEndYear()))),
                        competition.getStartYear(), competition.getEndYear())
                : simulationEngine.calculate(budget,
                        List.of(new SimulationEngine.AllocationInput(asset.getId(), budget, quotes)),
                        competition.getStartYear(), competition.getEndYear());
        return result.totalReturn();
    }
}
