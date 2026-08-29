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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de GET /api/rankings.
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

    @Test
    void deveRetornarRankingPorCompetitionIdOrdenadoPorRank() throws Exception {
        Competition rodada = criarCompeticao(1);
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
                .andExpect(jsonPath("$[1].username").value("bruno"))
                .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    void deveRetornarRankingPorNumeroDaRodada() throws Exception {
        Competition rodada1 = criarCompeticao(1);
        Competition rodada2 = criarCompeticao(2);
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

    @Test
    void deveRetornarRankingGlobalPorQueryParam() throws Exception {
        User terceiro = criarUsuario("caio", "caio@retrobolsa.com", 40);
        criarUsuario("bia", "bia@retrobolsa.com", 150);
        criarUsuario("ana", "ana@retrobolsa.com", 150);

        mockMvc.perform(get("/api/rankings")
                        .param("type", "global")
                        .header("Authorization", "Bearer " + gerarToken(terceiro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].username").value("ana"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].totalScore").value(150))
                .andExpect(jsonPath("$[1].username").value("bia"))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[2].username").value("caio"))
                .andExpect(jsonPath("$[2].totalScore").value(40));
    }

    @Test
    void deveRetornarRankingGlobalPelaRotaDedicada() throws Exception {
        User usuario = criarUsuario("duda", "duda@retrobolsa.com", 90);

        mockMvc.perform(get("/api/rankings/global")
                        .header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("duda"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].totalScore").value(90));
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaRankingParaRodada() throws Exception {
        Competition rodada = criarCompeticao(1);
        User usuario = criarUsuario("eva", "eva@retrobolsa.com", 10);

        mockMvc.perform(get("/api/rankings")
                        .param("competitionId", rodada.getId().toString())
                        .header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveRejeitarConsultaSemFiltro() throws Exception {
        User usuario = criarUsuario("fabio", "fabio@retrobolsa.com", 10);

        mockMvc.perform(get("/api/rankings")
                        .header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Informe competitionId ou roundNumber")));
    }

    @Test
    void deveRejeitarFiltrosConcorrentes() throws Exception {
        Competition rodada = criarCompeticao(1);
        User usuario = criarUsuario("gabi", "gabi@retrobolsa.com", 10);

        mockMvc.perform(get("/api/rankings")
                        .param("competitionId", rodada.getId().toString())
                        .param("roundNumber", "1")
                        .header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Use apenas um filtro")));
    }

    @Test
    void deveNegarAcessoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/rankings").param("roundNumber", "1"))
                .andExpect(status().isUnauthorized());
    }

    private Competition criarCompeticao(int round) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(round)
                .status("closed")
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

    private Portfolio criarPortfolio(User user, Competition competition, int rank, String totalReturn, String finalValue) {
        return portfolioRepository.save(Portfolio.builder()
                .user(user)
                .competition(competition)
                .rank(rank)
                .totalReturn(new BigDecimal(totalReturn))
                .finalValue(new BigDecimal(finalValue))
                .build());
    }

    private String gerarToken(User user) {
        return jwtUtil.generateToken(user.getEmail());
    }
}
