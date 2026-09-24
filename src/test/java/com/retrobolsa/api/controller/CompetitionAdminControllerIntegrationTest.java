package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.achievement.AchievementCodes;
import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de /api/admin/competitions: controle de acesso por role
 * e tratamento de UUID inválido em path variable (antes caía em 500 genérico).
 */
class CompetitionAdminControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AchievementService achievementService;

    @BeforeEach
    void limparBanco() {
        competitionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void deveRetornar400QuandoIdDaRodadaNaoEUuidValido() throws Exception {
        User admin = criarAdmin("admin@retrobolsa.com");

        mockMvc.perform(post("/api/admin/competitions/{id}/close", "isso-nao-e-um-uuid")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(admin.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Parametro invalido")));
    }

    @Test
    void deveNegarAcessoParaUsuarioSemRoleAdmin() throws Exception {
        User jogador = userRepository.save(User.builder()
                .username("jogador")
                .email("jogador@retrobolsa.com")
                .passwordHash(passwordEncoder.encode("senha123"))
                .role("PLAYER")
                .build());

        mockMvc.perform(post("/api/admin/competitions/next-round")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(jogador.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarAcessoSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/admin/competitions/next-round"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetApagaConquistasDeJogoMasMantemAsDeAulas() throws Exception {
        User admin = criarAdmin("admin@retrobolsa.com");
        User jogador = userRepository.save(User.builder()
                .username("jogador")
                .email("jogador@retrobolsa.com")
                .passwordHash(passwordEncoder.encode("senha123"))
                .build());
        // O reset exige uma rodada 1 para reabrir.
        competitionRepository.save(Competition.builder()
                .roundNumber(1)
                .status("simulated")
                .budget(new BigDecimal("100000.00"))
                .startYear(2020)
                .endYear(2022)
                .build());
        achievementService.unlock(jogador, List.of(
                AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.CAMPEAO_RODADA, AchievementCodes.PRIMEIRA_AULA));

        mockMvc.perform(post("/api/admin/competitions/reset")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(admin.getEmail())))
                .andExpect(status().isNoContent());

        // Carteiras e pontos somem no reset, mas o progresso das aulas não — então só a de aula fica.
        assertThat(achievementService.listForUser(jogador.getId()))
                .filteredOn(AchievementResponseDto::isUnlocked)
                .extracting(AchievementResponseDto::getCode)
                .containsExactly(AchievementCodes.PRIMEIRA_AULA);
    }

    private User criarAdmin(String email) {
        return userRepository.save(User.builder()
                .username("admin")
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .role("ADMIN")
                .build());
    }
}
