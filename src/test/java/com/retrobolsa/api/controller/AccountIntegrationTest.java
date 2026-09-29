package com.retrobolsa.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A conta do jogador: primeiro acesso, consentimento no cadastro, senha e LGPD. */
class AccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

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

    @Test
    @DisplayName("troca de senha logado: confere a atual, a confirmação e que a nova é diferente")
    void trocaDeSenha() throws Exception {
        cadastrar("ana", "");
        User ana = userRepository.findByEmail("ana@retrobolsa.com").orElseThrow();

        trocarSenha(ana, "errada-123", "nova-senha-9", "nova-senha-9").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A senha atual não confere."));
        trocarSenha(ana, "senha-forte-1", "nova-senha-9", "outra-coisa-9").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A confirmação é diferente da nova senha."));
        trocarSenha(ana, "senha-forte-1", "curta", "curta").andExpect(status().isBadRequest());
        trocarSenha(ana, "senha-forte-1", "senha-forte-1", "senha-forte-1").andExpect(status().isBadRequest());

        trocarSenha(ana, "senha-forte-1", "nova-senha-9", "nova-senha-9").andExpect(status().isNoContent());
        entrar("ana@retrobolsa.com", "senha-forte-1").andExpect(status().isBadRequest());
        entrar("ana@retrobolsa.com", "nova-senha-9").andExpect(status().isOk());
    }

    @Test
    @DisplayName("o admin gera uma senha temporária; com ela, o perfil pede a troca até a pessoa trocar")
    void redefinicaoPeloAdmin() throws Exception {
        cadastrar("beto", "");
        User beto = userRepository.findByEmail("beto@retrobolsa.com").orElseThrow();
        User admin = userRepository.save(User.builder().username("root").email("root@retrobolsa.com")
                .passwordHash("hash").role("ADMIN").build());

        String resposta = mockMvc.perform(post("/api/admin/users/" + beto.getId() + "/reset-password")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String temporaria = JsonPath.read(resposta, "$.temporaryPassword");
        assertThat(temporaria).matches("[a-km-np-z2-9]{10}");

        entrar("beto@retrobolsa.com", "senha-forte-1").andExpect(status().isBadRequest());
        entrar("beto@retrobolsa.com", temporaria).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/profile").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        trocarSenha(beto, temporaria, "minha-senha-7", "minha-senha-7").andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/profile").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        // Só o admin redefine, e não a senha de outro admin.
        mockMvc.perform(post("/api/admin/users/" + beto.getId() + "/reset-password").header("Authorization", bearer(beto)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/" + admin.getId() + "/reset-password").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("baixar os dados traz perfil, progresso, aulas, quizzes, carteiras e turmas")
    void baixarDados() throws Exception {
        cadastrar("ana", ",\"aceiteTermos\":true");
        User ana = userRepository.findByEmail("ana@retrobolsa.com").orElseThrow();
        mockMvc.perform(post("/api/articles/bbbbbbbb-0001-0000-0000-000000000001/quiz").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QuizRespostas.certas(jdbc, "bbbbbbbb-0001-0000-0000-000000000001")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/export").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("meus-dados-retrobolsa.json")))
                .andExpect(jsonPath("$.perfil.email").value("ana@retrobolsa.com"))
                .andExpect(jsonPath("$.perfil.aceiteDeIdadeOuAutorizacaoEm").isNotEmpty())
                .andExpect(jsonPath("$.progresso.xp").value(greaterThan(0)))
                .andExpect(jsonPath("$.aulasConcluidas[0].aula").value("O que é rentabilidade?"))
                .andExpect(jsonPath("$.quizzes[0].acertos").value(3))
                .andExpect(jsonPath("$.conquistas", not(empty())))
                .andExpect(jsonPath("$.carteiras", empty()))
                .andExpect(jsonPath("$.turmas", empty()))
                .andExpect(jsonPath("$.notificacoes", empty()));
    }

    @Test
    @DisplayName("excluir a conta pede a senha e apaga tudo o que é da pessoa")
    void excluirConta() throws Exception {
        cadastrar("ana", "");
        User ana = userRepository.findByEmail("ana@retrobolsa.com").orElseThrow();
        mockMvc.perform(post("/api/articles/bbbbbbbb-0001-0000-0000-000000000001/quiz").header("Authorization", bearer(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content(QuizRespostas.certas(jdbc, "bbbbbbbb-0001-0000-0000-000000000001")));

        mockMvc.perform(delete("/api/users/me").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"errada-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A senha não confere."));
        assertThat(userRepository.findById(ana.getId())).isPresent();

        mockMvc.perform(delete("/api/users/me").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"senha-forte-1\"}"))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(ana.getId())).isEmpty();
        for (String tabela : new String[]{"xp_events", "user_achievements", "user_article_progress", "user_quiz_attempts"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE user_id = ?", Integer.class, ana.getId()))
                    .as(tabela).isZero();
        }
        // O token de quem não existe mais não entra.
        mockMvc.perform(get("/api/users/profile").header("Authorization", bearer(ana))).andExpect(status().isUnauthorized());
    }

    private ResultActions trocarSenha(User usuario, String atual, String nova, String confirmacao)
            throws Exception {
        return mockMvc.perform(post("/api/users/me/password").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"" + atual + "\",\"novaSenha\":\"" + nova + "\",\"confirmarSenha\":\"" + confirmacao + "\"}"));
    }

    private ResultActions entrar(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
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
