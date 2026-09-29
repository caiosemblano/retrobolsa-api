package com.retrobolsa.api.controller;

import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A conta do jogador: primeiro acesso e consentimento no cadastro. */
class AccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void limpar() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("o cadastro grava quando a pessoa marcou a caixa de idade ou autorização")
    void consentimento() throws Exception {
        cadastrar("ana", ",\"aceiteTermos\":true");
        cadastrar("beto", "");

        assertThat(userRepository.findByEmail("ana@retrobolsa.com").orElseThrow().getConsentedAt()).isNotNull();
        assertThat(userRepository.findByEmail("beto@retrobolsa.com").orElseThrow().getConsentedAt()).isNull();
    }

    @Test
    @DisplayName("conta nova começa no primeiro acesso, e o passo a passo visto fica marcado")
    void primeiroAcesso() throws Exception {
        cadastrar("ana", ",\"aceiteTermos\":true");
        User ana = userRepository.findByEmail("ana@retrobolsa.com").orElseThrow();

        mockMvc.perform(get("/api/users/profile").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.onboarded").value(false))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        mockMvc.perform(post("/api/users/me/onboarded").header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/profile").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.onboarded").value(true));
        mockMvc.perform(post("/api/users/me/onboarded")).andExpect(status().isUnauthorized());
    }

    private void cadastrar(String nome, String extra) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + nome + "\",\"email\":\"" + nome + "@retrobolsa.com\","
                                + "\"senha\":\"senha-forte-1\",\"confirmarSenha\":\"senha-forte-1\"" + extra + "}"))
                .andExpect(status().isCreated());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
