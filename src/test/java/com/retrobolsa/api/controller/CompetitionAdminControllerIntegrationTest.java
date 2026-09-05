package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

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

    private User criarAdmin(String email) {
        return userRepository.save(User.builder()
                .username("admin")
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .role("ADMIN")
                .build());
    }
}
