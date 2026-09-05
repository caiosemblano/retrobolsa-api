package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.portfolio.AllocationRepository;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de GET /api/rankings.
 * Usa Testcontainers (PostgreSQL) via AbstractIntegrationTest.
 */
class RankingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void limparBanco() {
        allocationRepository.deleteAll();
        portfolioRepository.deleteAll();
        competitionRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Competition criarCompeticao(int round, String status) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(round)
                .status(status)
                .budget(new BigDecimal("100000.00"))
                .startYear(2020)
                .endYear(2023)
                .assets(List.of())
                .build());
    }

    private User criarUsuario(String username, String email, int totalScore) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .totalScore(totalScore)
                .build());
    }

    private Portfolio criarPortfolio(User user, Competition competition, Integer rank,
                                     String totalReturn, String finalValue) {
        return portfolioRepository.save(Portfolio.builder()
                .user(user)
                .competition(competition)
                .rank(rank)
                .totalReturn(totalReturn != null ? new BigDecimal(totalReturn) : null)
                .finalValue(finalValue != null ? new BigDecimal(finalValue) : null)
                .submittedAt(LocalDateTime.now())
                .build());
    }

    private Portfolio criarPortfolioComSubmissao(User user, Competition competition, Integer rank,
                                                  String totalReturn, LocalDateTime submittedAt) {
        return portfolioRepository.save(Portfolio.builder()
                .user(user)
                .competition(competition)
                .rank(rank)
                .totalReturn(totalReturn != null ? new BigDecimal(totalReturn) : null)
                .submittedAt(submittedAt)
                .build());
    }

    private String gerarToken(User user) {
        return jwtUtil.generateToken(user.getEmail());
    }

    // =========================================================================
    // GET /api/rankings?competitionId=...
    // =========================================================================

    @Nested
    @DisplayName("GET /api/rankings?competitionId")
    class GetRankingByCompetitionId {

        @Test
        @DisplayName("retorna ranking ordenado por rank quando competitionId é informado")
        void deveRetornarRankingPorCompetitionIdOrdenadoPorRank() throws Exception {
            Competition rodada = criarCompeticao(1, "simulated");
            User segundo = criarUsuario("bruno", "bruno@retrobolsa.com", 80);
            User primeiro = criarUsuario("ana", "ana@retrobolsa.com", 120);
            criarPortfolio(segundo, rodada, 2, "0.12", "112000.00");
            criarPortfolio(primeiro, rodada, 1, "0.25", "125000.00");

            mockMvc.perform(get("/api/rankings")
                            .param("competitionId", rodada.getId().toString())
                            .header("Authorization", "Bearer " + gerarToken(primeiro)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].username").value("ana"))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].totalReturn").value(0.25))
                    .andExpect(jsonPath("$[0].finalValue").value(125000.00))
                    .andExpect(jsonPath("$[0].roundNumber").value(1))
                    .andExpect(jsonPath("$[0].roundStatus").value("simulated"))
                    .andExpect(jsonPath("$[0].userId").isString())
                    .andExpect(jsonPath("$[1].username").value("bruno"))
                    .andExpect(jsonPath("$[1].rank").value(2));
        }

        @Test
        @DisplayName("retorna lista vazia quando não há portfólios na rodada")
        void deveRetornarListaVaziaQuandoNaoHaRankingParaRodada() throws Exception {
            Competition rodada = criarCompeticao(1, "simulated");
            User usuario = criarUsuario("eva", "eva@retrobolsa.com", 10);

            mockMvc.perform(get("/api/rankings")
                            .param("competitionId", rodada.getId().toString())
                            .header("Authorization", "Bearer " + gerarToken(usuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("retorna 400 quando competitionId não existe")
        void deveRetornar400ParaCompetitionIdInexistente() throws Exception {
            User usuario = criarUsuario("zara", "zara@retrobolsa.com", 10);

            mockMvc.perform(get("/api/rankings")
                            .param("competitionId", "00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + gerarToken(usuario)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro", containsString("Competição não encontrada")));
        }
    }

    // =========================================================================
    // GET /api/rankings?roundNumber=...
    // =========================================================================

    @Nested
    @DisplayName("GET /api/rankings?roundNumber")
    class GetRankingByRoundNumber {

        @Test
        @DisplayName("retorna ranking da rodada correta quando roundNumber é informado")
        void deveRetornarRankingPorNumeroDaRodada() throws Exception {
            Competition rodada1 = criarCompeticao(1, "simulated");
            Competition rodada2 = criarCompeticao(2, "simulated");
            User usuario = criarUsuario("carla", "carla@retrobolsa.com", 70);
            criarPortfolio(usuario, rodada1, 1, "0.10", "110000.00");
            criarPortfolio(usuario, rodada2, 1, "0.30", "130000.00");

            mockMvc.perform(get("/api/rankings")
                            .param("roundNumber", "2")
                            .header("Authorization", "Bearer " + gerarToken(usuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].username").value("carla"))
                    .andExpect(jsonPath("$[0].roundNumber").value(2))
                    .andExpect(jsonPath("$[0].totalReturn").value(0.30));
        }
    }

    // =========================================================================
    // GET /api/rankings?type=global
    // =========================================================================

    @Nested
    @DisplayName("GET /api/rankings?type=global")
    class GetRankingGlobal {

        @Test
        @DisplayName("retorna ranking global ordenado por totalScore com desempate por username")
        void deveRetornarRankingGlobalOrdenadoComDesempate() throws Exception {
            User terceiro = criarUsuario("caio", "caio@retrobolsa.com", 40);
            criarUsuario("bia", "bia@retrobolsa.com", 150);
            criarUsuario("ana", "ana@retrobolsa.com", 150);

            mockMvc.perform(get("/api/rankings")
                            .param("type", "global")
                            .header("Authorization", "Bearer " + gerarToken(terceiro)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    // ana e bia empatados em 150; desempate por username asc → ana vem primeiro
                    .andExpect(jsonPath("$[0].username").value("ana"))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].totalScore").value(150))
                    .andExpect(jsonPath("$[0].userId").isString())
                    .andExpect(jsonPath("$[0].competitionsPlayed").isNumber())
                    .andExpect(jsonPath("$[1].username").value("bia"))
                    .andExpect(jsonPath("$[1].rank").value(2))
                    .andExpect(jsonPath("$[2].username").value("caio"))
                    .andExpect(jsonPath("$[2].totalScore").value(40))
                    .andExpect(jsonPath("$[2].rank").value(3));
        }

        @Test
        @DisplayName("retorna ranking global pelo alias 'general'")
        void deveAceitarAliasGeneral() throws Exception {
            User u = criarUsuario("luna", "luna@retrobolsa.com", 90);

            mockMvc.perform(get("/api/rankings")
                            .param("type", "general")
                            .header("Authorization", "Bearer " + gerarToken(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("aceita parâmetros de paginação limit e page")
        void deveAceitarParametrosDePaginacao() throws Exception {
            User u1 = criarUsuario("alfa", "alfa@retrobolsa.com", 100);
            criarUsuario("beta", "beta@retrobolsa.com", 90);
            criarUsuario("gama", "gama@retrobolsa.com", 80);

            // Solicita apenas 2 por página → deve retornar só 2
            mockMvc.perform(get("/api/rankings")
                            .param("type", "global")
                            .param("limit", "2")
                            .param("page", "0")
                            .header("Authorization", "Bearer " + gerarToken(u1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // =========================================================================
    // GET /api/rankings/global (rota dedicada)
    // =========================================================================

    @Nested
    @DisplayName("GET /api/rankings/global")
    class GetGlobalDedicatedRoute {

        @Test
        @DisplayName("retorna ranking global pela rota dedicada")
        void deveRetornarRankingGlobalPelaRotaDedicada() throws Exception {
            User usuario = criarUsuario("duda", "duda@retrobolsa.com", 90);

            mockMvc.perform(get("/api/rankings/global")
                            .header("Authorization", "Bearer " + gerarToken(usuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].username").value("duda"))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].totalScore").value(90))
                    .andExpect(jsonPath("$[0].userId").isString());
        }

        @Test
        @DisplayName("aceita parâmetros de paginação na rota dedicada")
        void deveAceitarPaginacaoNaRotaDedicada() throws Exception {
            User u = criarUsuario("igor", "igor@retrobolsa.com", 50);
            criarUsuario("joao", "joao@retrobolsa.com", 40);

            mockMvc.perform(get("/api/rankings/global")
                            .param("limit", "1")
                            .param("page", "0")
                            .header("Authorization", "Bearer " + gerarToken(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].username").value("igor"));
        }
    }

    // =========================================================================
    // GET /api/rankings?type=quinzenal
    // =========================================================================

    @Nested
    @DisplayName("GET /api/rankings?type=quinzenal")
    class GetRankingQuinzenal {

        @Test
        @DisplayName("retorna portfólios da rodada open com roundStatus=open")
        void deveRetornarParticipantesDaRodadaAberta() throws Exception {
            Competition open = criarCompeticao(5, "open");
            User u1 = criarUsuario("karen", "karen@retrobolsa.com", 50);
            User u2 = criarUsuario("leo", "leo@retrobolsa.com", 60);
            criarPortfolio(u1, open, null, null, null);
            criarPortfolio(u2, open, null, null, null);

            mockMvc.perform(get("/api/rankings")
                            .param("type", "quinzenal")
                            .header("Authorization", "Bearer " + gerarToken(u1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].roundStatus").value("open"));
        }

        @Test
        @DisplayName("faz fallback para última rodada simulada quando não há rodada open")
        void deveFazerFallbackParaUltimaRodadaSimulada() throws Exception {
            Competition s1 = criarCompeticao(1, "simulated");
            Competition s2 = criarCompeticao(2, "simulated");
            User u = criarUsuario("maria", "maria@retrobolsa.com", 100);
            criarPortfolio(u, s1, 1, "0.10", "110000");
            criarPortfolio(u, s2, 1, "0.20", "120000");

            mockMvc.perform(get("/api/rankings")
                            .param("type", "quinzenal")
                            .header("Authorization", "Bearer " + gerarToken(u)))
                    .andExpect(status().isOk())
                    // deve retornar apenas a rodada 2 (a mais recente)
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].roundNumber").value(2))
                    .andExpect(jsonPath("$[0].roundStatus").value("simulated"));
        }

        @Test
        @DisplayName("retorna lista vazia quando não há nenhuma rodada")
        void deveRetornarListaVaziaQuandoNaoHaRodada() throws Exception {
            User u = criarUsuario("nara", "nara@retrobolsa.com", 10);

            mockMvc.perform(get("/api/rankings")
                            .param("type", "quinzenal")
                            .header("Authorization", "Bearer " + gerarToken(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // GET /api/rankings/me
    // =========================================================================

    @Nested
    @DisplayName("GET /api/rankings/me")
    class GetMyRank {

        @Test
        @DisplayName("retorna posição global do usuário autenticado")
        void deveRetornarPosicaoDoUsuarioAutenticado() throws Exception {
            User u1 = criarUsuario("pedro", "pedro@retrobolsa.com", 200);
            User u2 = criarUsuario("quin", "quin@retrobolsa.com", 100);

            mockMvc.perform(get("/api/rankings/me")
                            .header("Authorization", "Bearer " + gerarToken(u2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("quin"))
                    .andExpect(jsonPath("$.globalRank").value(2))
                    .andExpect(jsonPath("$.totalGlobalPlayers").value(2))
                    .andExpect(jsonPath("$.totalScore").value(100))
                    .andExpect(jsonPath("$.userId").isString());
        }

        @Test
        @DisplayName("retorna posição na rodada ativa quando o usuário participou")
        void deveRetornarPosicaoNaRodadaAtiva() throws Exception {
            Competition simulated = criarCompeticao(3, "simulated");
            User u = criarUsuario("raul", "raul@retrobolsa.com", 150);
            criarPortfolio(u, simulated, 1, "0.30", "130000");

            mockMvc.perform(get("/api/rankings/me")
                            .header("Authorization", "Bearer " + gerarToken(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeRoundRank").value(1))
                    .andExpect(jsonPath("$.activeRoundNumber").value(3))
                    .andExpect(jsonPath("$.activeRoundStatus").value("simulated"));
        }

        @Test
        @DisplayName("retorna activeRoundRank nulo quando usuário não participou de nenhuma rodada")
        void deveRetornarActiveRoundRankNuloQuandoNaoParticipou() throws Exception {
            criarCompeticao(1, "simulated"); // rodada existe, mas user não participou
            User u = criarUsuario("sara", "sara@retrobolsa.com", 50);

            mockMvc.perform(get("/api/rankings/me")
                            .header("Authorization", "Bearer " + gerarToken(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeRoundRank").doesNotExist());
        }

        @Test
        @DisplayName("retorna 401 quando chamado sem autenticação")
        void deveNegarAcessoSemAutenticacao() throws Exception {
            mockMvc.perform(get("/api/rankings/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Validações de parâmetros e erros
    // =========================================================================

    @Nested
    @DisplayName("Validação de parâmetros")
    class ValidacaoDeParametros {

        @Test
        @DisplayName("retorna 400 quando nenhum filtro é informado (sem type)")
        void deveRejeitarConsultaSemFiltro() throws Exception {
            User usuario = criarUsuario("fabio", "fabio@retrobolsa.com", 10);

            mockMvc.perform(get("/api/rankings")
                            .header("Authorization", "Bearer " + gerarToken(usuario)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro", containsString("Informe competitionId ou roundNumber")));
        }

        @Test
        @DisplayName("retorna 400 quando competitionId e roundNumber são informados simultaneamente")
        void deveRejeitarFiltrosConcorrentes() throws Exception {
            Competition rodada = criarCompeticao(1, "simulated");
            User usuario = criarUsuario("gabi", "gabi@retrobolsa.com", 10);

            mockMvc.perform(get("/api/rankings")
                            .param("competitionId", rodada.getId().toString())
                            .param("roundNumber", "1")
                            .header("Authorization", "Bearer " + gerarToken(usuario)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro", containsString("Use apenas um filtro")));
        }

        @Test
        @DisplayName("permite acesso sem autenticação (rota sem type é pública)")
        void devePermitirAcessoSemAutenticacaoRotaSimples() throws Exception {
            // /api/rankings é pública (apenas /api/rankings/me exige autenticação).
            mockMvc.perform(get("/api/rankings").param("roundNumber", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // =========================================================================
    // Temporada (type=season e /season/current)
    // =========================================================================

    @Nested
    @DisplayName("Ranking de temporada")
    class SeasonRanking {

        @Test
        @DisplayName("soma apenas as rodadas da temporada atual, ignorando as anteriores")
        void deveConsiderarApenasRodadasDaTemporadaAtual() throws Exception {
            Competition rodada4 = criarCompeticao(4, "simulated"); // temporada 1
            Competition rodada5 = criarCompeticao(5, "simulated"); // temporada 2 (atual)
            Competition rodada6 = criarCompeticao(6, "simulated"); // temporada 2 (atual)
            User ana = criarUsuario("ana", "ana@retrobolsa.com", 500);
            User bruno = criarUsuario("bruno", "bruno@retrobolsa.com", 500);

            criarPortfolio(ana, rodada4, 1, "90.00", "190000.00");   // temporada anterior: ignorado
            criarPortfolio(ana, rodada5, 2, "10.40", "110400.00");   // 10 pontos
            criarPortfolio(ana, rodada6, 1, "25.60", "125600.00");   // 26 pontos
            criarPortfolio(bruno, rodada5, 1, "30.00", "130000.00"); // 30 pontos

            mockMvc.perform(get("/api/rankings").param("type", "season"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].username").value("ana"))
                    .andExpect(jsonPath("$[0].rank").value(1))
                    .andExpect(jsonPath("$[0].totalScore").value(36))
                    .andExpect(jsonPath("$[0].competitionsPlayed").value(2))
                    .andExpect(jsonPath("$[1].username").value("bruno"))
                    .andExpect(jsonPath("$[1].totalScore").value(30));
        }

        @Test
        @DisplayName("ignora rodadas ainda não simuladas e contas ADMIN")
        void deveIgnorarRodadasAbertasEAdmins() throws Exception {
            Competition simulada = criarCompeticao(1, "simulated");
            Competition aberta = criarCompeticao(2, "open");
            User ana = criarUsuario("ana", "ana@retrobolsa.com", 0);
            User admin = userRepository.save(User.builder()
                    .username("Administrador")
                    .email("admin@admin.com")
                    .passwordHash(passwordEncoder.encode("senha123"))
                    .role("ADMIN")
                    .totalScore(999)
                    .build());

            criarPortfolio(ana, simulada, 1, "20.00", "120000.00");
            criarPortfolio(ana, aberta, null, null, null);
            criarPortfolio(admin, simulada, 2, "80.00", "180000.00");

            mockMvc.perform(get("/api/rankings").param("type", "season"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].username").value("ana"))
                    .andExpect(jsonPath("$[0].totalScore").value(20))
                    .andExpect(jsonPath("$[0].competitionsPlayed").value(1));
        }

        @Test
        @DisplayName("GET /api/rankings/season/current retorna a faixa de rodadas da temporada")
        void deveRetornarInfoDaTemporadaAtual() throws Exception {
            criarCompeticao(5, "open");

            mockMvc.perform(get("/api/rankings/season/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seasonNumber").value(2))
                    .andExpect(jsonPath("$.roundStart").value(5))
                    .andExpect(jsonPath("$.roundEnd").value(8));
        }

        @Test
        @DisplayName("GET /api/rankings/season/current assume temporada 1 sem rodadas cadastradas")
        void deveRetornarTemporada1SemRodadas() throws Exception {
            mockMvc.perform(get("/api/rankings/season/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.seasonNumber").value(1))
                    .andExpect(jsonPath("$.roundStart").value(1))
                    .andExpect(jsonPath("$.roundEnd").value(4));
        }
    }
}
