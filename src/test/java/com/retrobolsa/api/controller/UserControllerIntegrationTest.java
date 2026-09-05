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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de GET /api/users/profile.
 * Usa Testcontainers (PostgreSQL) via AbstractIntegrationTest.
 */
class UserControllerIntegrationTest extends AbstractIntegrationTest {

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

    private Competition criarCompeticao(int round, String cenario) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(round)
                .status("simulated")
                .budget(new BigDecimal("100000.00"))
                .scenarioTitle(cenario)
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

    private void criarPortfolio(User user, Competition competition, Integer rank, String totalReturn) {
        portfolioRepository.save(Portfolio.builder()
                .user(user)
                .competition(competition)
                .rank(rank)
                .totalReturn(new BigDecimal(totalReturn))
                .finalValue(new BigDecimal("110000.00"))
                .submittedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("retorna perfil com pontuação, melhor posição e histórico ordenado por rodada desc")
    void deveRetornarPerfilCompleto() throws Exception {
        User ana = criarUsuario("ana", "ana@retrobolsa.com", 46);
        Competition rodada1 = criarCompeticao(1, "Plano Real");
        Competition rodada2 = criarCompeticao(2, "Crise de 2008");
        criarPortfolio(ana, rodada1, 3, "20.00");
        criarPortfolio(ana, rodada2, 1, "26.00");

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(ana.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.email").value("ana@retrobolsa.com"))
                .andExpect(jsonPath("$.totalScore").value(46))
                .andExpect(jsonPath("$.bestRank").value(1))
                .andExpect(jsonPath("$.competitions").value(2))
                .andExpect(jsonPath("$.history", hasSize(2)))
                .andExpect(jsonPath("$.history[0].roundNumber").value(2))
                .andExpect(jsonPath("$.history[0].scenarioTitle").value("Crise de 2008"))
                .andExpect(jsonPath("$.history[0].rank").value(1))
                .andExpect(jsonPath("$.history[1].roundNumber").value(1))
                .andExpect(jsonPath("$.history[1].scenarioTitle").value("Plano Real"));
    }

    @Test
    @DisplayName("retorna perfil sem histórico nem melhor posição para usuário que nunca jogou")
    void deveRetornarPerfilVazioParaNovoUsuario() throws Exception {
        User novato = criarUsuario("novato", "novato@retrobolsa.com", 0);

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(novato.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0))
                .andExpect(jsonPath("$.bestRank").doesNotExist())
                .andExpect(jsonPath("$.competitions").value(0))
                .andExpect(jsonPath("$.history", hasSize(0)));
    }

    @Test
    @DisplayName("não expõe o perfil de outro usuário — os dados vêm do token")
    void deveRetornarApenasDadosDoUsuarioAutenticado() throws Exception {
        User ana = criarUsuario("ana", "ana@retrobolsa.com", 100);
        criarUsuario("bruno", "bruno@retrobolsa.com", 500);
        Competition rodada = criarCompeticao(1, "Plano Real");
        criarPortfolio(ana, rodada, 2, "15.00");

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(ana.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.totalScore").value(100));
    }

    @Test
    @DisplayName("exige autenticação")
    void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }
}
