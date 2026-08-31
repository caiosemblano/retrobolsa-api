package com.retrobolsa.api.game.ranking;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.GlobalRankingResponseDto;
import com.retrobolsa.api.game.dto.RankingResponseDto;
import com.retrobolsa.api.game.dto.UserRankSummaryDto;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService — Testes Unitários")
class RankingServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private RankingService rankingService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private User buildUser(String username, int totalScore) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(username + "@retrobolsa.com")
                .totalScore(totalScore)
                .build();
    }

    private Competition buildCompetition(int roundNumber, String status) {
        return Competition.builder()
                .id(UUID.randomUUID())
                .roundNumber(roundNumber)
                .status(status)
                .budget(new BigDecimal("100000.00"))
                .startYear(2020)
                .endYear(2023)
                .build();
    }

    private Portfolio buildPortfolio(User user, Competition competition, Integer rank,
                                     BigDecimal totalReturn, BigDecimal finalValue) {
        return Portfolio.builder()
                .id(UUID.randomUUID())
                .user(user)
                .competition(competition)
                .rank(rank)
                .totalReturn(totalReturn)
                .finalValue(finalValue)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // getQuinzenalRanking
    // =========================================================================

    @Nested
    @DisplayName("getQuinzenalRanking()")
    class GetQuinzenalRanking {

        @Test
        @DisplayName("retorna participantes da rodada aberta quando há uma rodada open")
        void deveRetornarParticipantesDaRodadaAberta() {
            Competition open = buildCompetition(3, "open");
            User u1 = buildUser("ana", 100);
            User u2 = buildUser("beto", 80);
            Portfolio p1 = buildPortfolio(u1, open, null, null, null);
            Portfolio p2 = buildPortfolio(u2, open, null, null, null);

            when(competitionRepository.findByStatus("open")).thenReturn(Optional.of(open));
            when(portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(open.getId()))
                    .thenReturn(List.of(p1, p2));

            List<RankingResponseDto> result = rankingService.getQuinzenalRanking();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRoundStatus()).isEqualTo("open");
            assertThat(result.get(0).getRank()).isEqualTo(0); // rank não calculado ainda
            verify(competitionRepository, never()).findTopByStatusInOrderByRoundNumberDesc(any());
        }

        @Test
        @DisplayName("faz fallback para última rodada finalizada quando não há rodada open")
        void deveFazerFallbackParaUltimaRodadaFinalizada() {
            Competition simulated = buildCompetition(2, "simulated");
            User u1 = buildUser("carla", 150);
            Portfolio p1 = buildPortfolio(u1, simulated, 1, new BigDecimal("0.15"), new BigDecimal("115000"));

            when(competitionRepository.findByStatus("open")).thenReturn(Optional.empty());
            when(competitionRepository.findTopByStatusInOrderByRoundNumberDesc(anyList()))
                    .thenReturn(Optional.of(simulated));
            when(portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(simulated.getId()))
                    .thenReturn(List.of(p1));

            List<RankingResponseDto> result = rankingService.getQuinzenalRanking();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoundStatus()).isEqualTo("simulated");
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(0).getUsername()).isEqualTo("carla");
        }

        @Test
        @DisplayName("retorna lista vazia quando não há rodada alguma")
        void deveRetornarListaVaziaQuandoNaoHaRodada() {
            when(competitionRepository.findByStatus("open")).thenReturn(Optional.empty());
            when(competitionRepository.findTopByStatusInOrderByRoundNumberDesc(anyList()))
                    .thenReturn(Optional.empty());

            List<RankingResponseDto> result = rankingService.getQuinzenalRanking();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getRanking
    // =========================================================================

    @Nested
    @DisplayName("getRanking(competitionId, roundNumber)")
    class GetRanking {

        @Test
        @DisplayName("lança exceção quando nenhum filtro é informado")
        void deveLancarExcecaoSemFiltro() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> rankingService.getRanking(null, null))
                    .withMessageContaining("Informe competitionId ou roundNumber");
        }

        @Test
        @DisplayName("lança exceção quando ambos os filtros são informados")
        void deveLancarExcecaoComAmbosFiltros() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> rankingService.getRanking(UUID.randomUUID(), 1))
                    .withMessageContaining("Use apenas um filtro");
        }

        @Test
        @DisplayName("lança exceção quando competitionId não é encontrado")
        void deveLancarExcecaoQuandoCompetitionIdNaoExiste() {
            UUID fakeId = UUID.randomUUID();
            when(competitionRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> rankingService.getRanking(fakeId, null))
                    .withMessageContaining("Competição não encontrada");
        }

        @Test
        @DisplayName("retorna ranking por competitionId com desempate correto")
        void deveRetornarRankingPorCompetitionId() {
            Competition comp = buildCompetition(1, "simulated");
            User u1 = buildUser("ana", 100);
            User u2 = buildUser("beto", 80);
            Portfolio p1 = buildPortfolio(u1, comp, 1, new BigDecimal("0.25"), new BigDecimal("125000"));
            Portfolio p2 = buildPortfolio(u2, comp, 2, new BigDecimal("0.12"), new BigDecimal("112000"));

            when(competitionRepository.findById(comp.getId())).thenReturn(Optional.of(comp));
            when(portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(comp.getId()))
                    .thenReturn(List.of(p1, p2));

            List<RankingResponseDto> result = rankingService.getRanking(comp.getId(), null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("ana");
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(1).getUsername()).isEqualTo("beto");
            assertThat(result.get(1).getRank()).isEqualTo(2);
        }

        @Test
        @DisplayName("retorna ranking por roundNumber")
        void deveRetornarRankingPorRoundNumber() {
            Competition comp = buildCompetition(5, "revealed");
            User u1 = buildUser("duda", 200);
            Portfolio p1 = buildPortfolio(u1, comp, 1, new BigDecimal("0.30"), new BigDecimal("130000"));

            when(portfolioRepository.findByRoundNumberOrderByRankThenTieBreak(5))
                    .thenReturn(List.of(p1));

            List<RankingResponseDto> result = rankingService.getRanking(null, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("duda");
            assertThat(result.get(0).getRoundNumber()).isEqualTo(5);
            assertThat(result.get(0).getRoundStatus()).isEqualTo("revealed");
        }

        @Test
        @DisplayName("retorna lista vazia para rodada sem participantes")
        void deveRetornarListaVaziaParaRodadaSemParticipantes() {
            Competition comp = buildCompetition(1, "simulated");
            when(competitionRepository.findById(comp.getId())).thenReturn(Optional.of(comp));
            when(portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(comp.getId()))
                    .thenReturn(List.of());

            List<RankingResponseDto> result = rankingService.getRanking(comp.getId(), null);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getGlobalRanking
    // =========================================================================

    @Nested
    @DisplayName("getGlobalRanking(limit, page)")
    class GetGlobalRanking {

        @Test
        @DisplayName("retorna usuários ordenados com rank sequencial correto")
        void deveRetornarRankingGlobalOrdenado() {
            User u1 = buildUser("ana", 150);
            User u2 = buildUser("bia", 150); // empate: desempate por username
            User u3 = buildUser("caio", 40);

            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc(any(PageRequest.class)))
                    .thenReturn(List.of(u1, u2, u3));
            when(portfolioRepository.countByUserId(any())).thenReturn(3L);

            List<GlobalRankingResponseDto> result = rankingService.getGlobalRanking(50, 0);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getUsername()).isEqualTo("ana");
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(0).getTotalScore()).isEqualTo(150);
            assertThat(result.get(1).getUsername()).isEqualTo("bia");
            assertThat(result.get(1).getRank()).isEqualTo(2);
            assertThat(result.get(2).getUsername()).isEqualTo("caio");
            assertThat(result.get(2).getRank()).isEqualTo(3);
            assertThat(result.get(2).getTotalScore()).isEqualTo(40);
        }

        @Test
        @DisplayName("calcula rank offset corretamente para página 2")
        void deveCalcularRankOffsetNaPagina2() {
            User u1 = buildUser("xavier", 10);

            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc(any(PageRequest.class)))
                    .thenReturn(List.of(u1));
            when(portfolioRepository.countByUserId(any())).thenReturn(1L);

            List<GlobalRankingResponseDto> result = rankingService.getGlobalRanking(10, 1);

            // página 1 (base 0) com pageSize 10 → offset = 10, logo rank = 11
            assertThat(result.get(0).getRank()).isEqualTo(11);
        }

        @Test
        @DisplayName("usa valores padrão quando limit e page são nulos")
        void deveUsarValoresPadraoComParametrosNulos() {
            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc(any(PageRequest.class)))
                    .thenReturn(List.of());

            rankingService.getGlobalRanking(null, null);

            // Verifica que usou PageRequest(0, 50) como padrão
            verify(userRepository).findAllByOrderByTotalScoreDescUsernameAsc(PageRequest.of(0, 50));
        }

        @Test
        @DisplayName("limita o pageSize a 200 mesmo quando limit maior é informado")
        void deveLimitarPageSizeA200() {
            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc(any(PageRequest.class)))
                    .thenReturn(List.of());

            rankingService.getGlobalRanking(9999, 0);

            verify(userRepository).findAllByOrderByTotalScoreDescUsernameAsc(PageRequest.of(0, 200));
        }

        @Test
        @DisplayName("inclui competitionsPlayed corretamente")
        void deveIncluirCompetitionsPlayed() {
            User u1 = buildUser("eva", 100);
            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc(any(PageRequest.class)))
                    .thenReturn(List.of(u1));
            when(portfolioRepository.countByUserId(u1.getId())).thenReturn(5L);

            List<GlobalRankingResponseDto> result = rankingService.getGlobalRanking(10, 0);

            assertThat(result.get(0).getCompetitionsPlayed()).isEqualTo(5);
        }
    }

    // =========================================================================
    // getUserRankSummary
    // =========================================================================

    @Nested
    @DisplayName("getUserRankSummary(email)")
    class GetUserRankSummary {

        @Test
        @DisplayName("lança exceção quando usuário não é encontrado pelo email")
        void deveLancarExcecaoParaEmailDesconhecido() {
            when(userRepository.findByEmail("desconhecido@retrobolsa.com"))
                    .thenReturn(Optional.empty());

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> rankingService.getUserRankSummary("desconhecido@retrobolsa.com"))
                    .withMessageContaining("Usuário não encontrado");
        }

        @Test
        @DisplayName("calcula posição global corretamente")
        void deveCalcularPosicaoGlobalCorreta() {
            User u1 = buildUser("ana", 200);
            User u2 = buildUser("beto", 150);
            User u3 = buildUser("carla", 80);

            when(userRepository.findByEmail(u2.getEmail())).thenReturn(Optional.of(u2));
            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc())
                    .thenReturn(List.of(u1, u2, u3));
            when(portfolioRepository.countByUserId(u2.getId())).thenReturn(2L);
            when(competitionRepository.findByStatus("open")).thenReturn(Optional.empty());
            when(competitionRepository.findTopByStatusInOrderByRoundNumberDesc(anyList()))
                    .thenReturn(Optional.empty());

            UserRankSummaryDto summary = rankingService.getUserRankSummary(u2.getEmail());

            assertThat(summary.getGlobalRank()).isEqualTo(2);
            assertThat(summary.getTotalGlobalPlayers()).isEqualTo(3);
            assertThat(summary.getCompetitionsPlayed()).isEqualTo(2);
            assertThat(summary.getActiveRoundRank()).isNull();
        }

        @Test
        @DisplayName("retorna posição na rodada ativa quando usuário participou")
        void deveRetornarPosicaoNaRodadaAtivaComRankCalculado() {
            User u1 = buildUser("duda", 100);
            Competition simulated = buildCompetition(3, "simulated");
            Portfolio p = buildPortfolio(u1, simulated, 2, new BigDecimal("0.10"), new BigDecimal("110000"));

            when(userRepository.findByEmail(u1.getEmail())).thenReturn(Optional.of(u1));
            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc())
                    .thenReturn(List.of(u1));
            when(portfolioRepository.countByUserId(u1.getId())).thenReturn(1L);
            when(competitionRepository.findByStatus("open")).thenReturn(Optional.empty());
            when(competitionRepository.findTopByStatusInOrderByRoundNumberDesc(anyList()))
                    .thenReturn(Optional.of(simulated));
            when(portfolioRepository.findByUserIdAndCompetitionId(u1.getId(), simulated.getId()))
                    .thenReturn(Optional.of(p));

            UserRankSummaryDto summary = rankingService.getUserRankSummary(u1.getEmail());

            assertThat(summary.getActiveRoundRank()).isEqualTo(2);
            assertThat(summary.getActiveRoundNumber()).isEqualTo(3);
            assertThat(summary.getActiveRoundStatus()).isEqualTo("simulated");
        }

        @Test
        @DisplayName("calcula posição dinâmica na rodada aberta quando rank ainda é nulo")
        void deveCalcularPosicaoDinamicaNaRodadaAberta() {
            User u1 = buildUser("eva", 50);
            User u2 = buildUser("fabio", 50);
            Competition open = buildCompetition(4, "open");
            Portfolio pEva = buildPortfolio(u1, open, null, null, null);
            Portfolio pFabio = buildPortfolio(u2, open, null, null, null);
            pFabio = Portfolio.builder()
                    .id(UUID.randomUUID())
                    .user(u2)
                    .competition(open)
                    .rank(null)
                    .submittedAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            when(userRepository.findByEmail(u1.getEmail())).thenReturn(Optional.of(u1));
            when(userRepository.findAllByOrderByTotalScoreDescUsernameAsc())
                    .thenReturn(List.of(u1, u2));
            when(portfolioRepository.countByUserId(u1.getId())).thenReturn(1L);
            when(competitionRepository.findByStatus("open")).thenReturn(Optional.of(open));
            when(portfolioRepository.findByUserIdAndCompetitionId(u1.getId(), open.getId()))
                    .thenReturn(Optional.of(pEva));
            // Fabio submeteu primeiro (minusMinutes), eva submeteu agora → eva na posição 2
            when(portfolioRepository.findByCompetitionIdOrderByRankThenTieBreak(open.getId()))
                    .thenReturn(List.of(pFabio, pEva));

            UserRankSummaryDto summary = rankingService.getUserRankSummary(u1.getEmail());

            assertThat(summary.getActiveRoundRank()).isEqualTo(2);
            assertThat(summary.getActiveRoundStatus()).isEqualTo("open");
        }
    }
}
