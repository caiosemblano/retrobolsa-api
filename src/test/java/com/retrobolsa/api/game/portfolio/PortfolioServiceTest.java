package com.retrobolsa.api.game.portfolio;

import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.asset.HistoricalQuote;
import com.retrobolsa.api.game.asset.HistoricalQuoteRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.simulation.SimulationEngine;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService — Simulação e Pontuação")
class PortfolioServiceTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private AllocationRepository allocationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private HistoricalQuoteRepository quoteRepository;
    @Mock private AssetSnapshotRepository snapshotRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimulationEngine simulationEngine;

    @InjectMocks private PortfolioService portfolioService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private final Asset asset = Asset.builder().id(UUID.randomUUID()).build();

    private Competition buildCompetition(String status) {
        return Competition.builder()
                .id(UUID.randomUUID())
                .roundNumber(1)
                .status(status)
                .budget(new BigDecimal("100000.00"))
                .startYear(2020)
                .endYear(2023)
                .build();
    }

    private Portfolio buildPortfolio(User user, Competition competition) {
        Portfolio portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(user)
                .competition(competition)
                .submittedAt(LocalDateTime.now())
                .build();
        portfolio.setAllocations(List.of(Allocation.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .asset(asset)
                .amountInvested(new BigDecimal("50000.00"))
                .percentWeight(new BigDecimal("0.5000"))
                .build()));
        return portfolio;
    }

    private void stubQuotes() {
        when(quoteRepository.findAllByAssetIdInAndDateBetweenOrderByAssetIdAscDateAsc(any(), any(), any()))
                .thenReturn(List.of(HistoricalQuote.builder()
                        .id(UUID.randomUUID())
                        .asset(asset)
                        .date(LocalDate.of(2019, 12, 31))
                        .closePrice(10.0)
                        .build()));
    }

    private void stubEngine(BigDecimal... totalReturnsInOrder) {
        SimulationEngine.SimulationResult first = result(totalReturnsInOrder[0]);
        SimulationEngine.SimulationResult[] rest = new SimulationEngine.SimulationResult[totalReturnsInOrder.length - 1];
        for (int i = 1; i < totalReturnsInOrder.length; i++) {
            rest[i - 1] = result(totalReturnsInOrder[i]);
        }
        when(simulationEngine.calculate(any(), any(), anyInt(), anyInt())).thenReturn(first, rest);
    }

    private SimulationEngine.SimulationResult result(BigDecimal totalReturn) {
        BigDecimal finalValue = new BigDecimal("100000.00")
                .multiply(BigDecimal.ONE.add(totalReturn.movePointLeft(2)));
        return new SimulationEngine.SimulationResult(
                finalValue, totalReturn, totalReturn, List.of(), List.of());
    }

    // =========================================================================
    // simulateCompetition
    // =========================================================================

    @Nested
    @DisplayName("simulateCompetition()")
    class SimulateCompetition {

        @Test
        @DisplayName("recusa simular rodada que não está fechada")
        void deveRecusarRodadaNaoFechada() {
            Competition open = buildCompetition("open");

            assertThatThrownBy(() -> portfolioService.simulateCompetition(open))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fechada");

            verify(competitionRepository, never()).save(any());
        }

        @Test
        @DisplayName("classifica por rentabilidade desc e credita pontos arredondados a cada usuário")
        void deveClassificarECreditarPontos() {
            Competition competition = buildCompetition("closed");
            User ana = User.builder().id(UUID.randomUUID()).username("ana").totalScore(10).build();
            User beto = User.builder().id(UUID.randomUUID()).username("beto").totalScore(0).build();
            Portfolio anaPortfolio = buildPortfolio(ana, competition);
            Portfolio betoPortfolio = buildPortfolio(beto, competition);

            // 1ª leitura: ordem de submissão. 2ª leitura (recalculateRanks): já ordenada
            // por totalReturn desc, como o banco devolveria após a simulação.
            when(portfolioRepository.findByCompetitionIdOrderByTotalReturnDesc(competition.getId()))
                    .thenReturn(List.of(anaPortfolio, betoPortfolio),
                            List.of(betoPortfolio, anaPortfolio));
            stubQuotes();
            stubEngine(new BigDecimal("12.40"), new BigDecimal("25.60"));

            portfolioService.simulateCompetition(competition);

            assertThat(betoPortfolio.getRank()).isEqualTo(1);
            assertThat(anaPortfolio.getRank()).isEqualTo(2);
            assertThat(anaPortfolio.getTotalReturn()).isEqualByComparingTo("12.40");
            assertThat(betoPortfolio.getTotalReturn()).isEqualByComparingTo("25.60");
            // 25.60 -> 26 pontos; 12.40 -> 12 pontos somados aos 10 já existentes
            assertThat(beto.getTotalScore()).isEqualTo(26);
            assertThat(ana.getTotalScore()).isEqualTo(22);
            assertThat(competition.getStatus()).isEqualTo("simulated");
        }

        @Test
        @DisplayName("pontuação acumulada nunca fica negativa após uma rodada ruim")
        void deveAplicarPisoZeroNaPontuacao() {
            Competition competition = buildCompetition("closed");
            User ana = User.builder().id(UUID.randomUUID()).username("ana").totalScore(5).build();
            Portfolio anaPortfolio = buildPortfolio(ana, competition);

            when(portfolioRepository.findByCompetitionIdOrderByTotalReturnDesc(competition.getId()))
                    .thenReturn(List.of(anaPortfolio));
            stubQuotes();
            stubEngine(new BigDecimal("-40.00"));

            portfolioService.simulateCompetition(competition);

            assertThat(ana.getTotalScore()).isZero();
            assertThat(anaPortfolio.getRank()).isEqualTo(1);
        }
    }
}
