package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.achievement.AchievementCodes;
import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.education.UserArticleProgressRepository;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de /api/articles contra o seed real (V3 + V10): os 8 artigos
 * e 3 módulos vêm das migrations, não de fixtures — então o teste também garante
 * que o V10 preencheu vídeo e conteúdo de todas as aulas.
 */
class ArticleControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String AULA_RENTABILIDADE = "bbbbbbbb-0001-0000-0000-000000000001";
    private static final String AULA_JUROS = "bbbbbbbb-0002-0000-0000-000000000002";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserArticleProgressRepository progressRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AchievementService achievementService;

    private User ana;
    private String token;

    @BeforeEach
    void preparar() {
        // Só usuários: artigos e módulos são o seed das migrations e não podem ser apagados.
        userRepository.deleteAll();
        ana = userRepository.save(User.builder()
                .username("ana")
                .email("ana@retrobolsa.com")
                .passwordHash("hash")
                .build());
        token = "Bearer " + jwtUtil.generateToken(ana.getEmail());
    }

    @Test
    @DisplayName("exige autenticação: 401 sem token (antes a rota era pública e respondia 400)")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/articles")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/articles/{id}/complete", AULA_RENTABILIDADE)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("lista as 8 aulas na ordem, cada uma com vídeo, conteúdo e dados reais do módulo")
    void listaAulasComVideoEConteudo() throws Exception {
        mockMvc.perform(get("/api/articles").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[*].videoId", everyItem(matchesPattern("^[A-Za-z0-9_-]{11}$"))))
                .andExpect(jsonPath("$[*].content", everyItem(not(blankOrNullString()))))
                .andExpect(jsonPath("$[*].moduleDescription", everyItem(not(blankOrNullString()))))
                .andExpect(jsonPath("$[*].completed", everyItem(is(false))))
                // Primeira aula do primeiro módulo
                .andExpect(jsonPath("$[0].title").value("O que é rentabilidade?"))
                .andExpect(jsonPath("$[0].videoId").value("Y9ng5fVji-A"))
                .andExpect(jsonPath("$[0].moduleTitle").value("Matemática Financeira"))
                .andExpect(jsonPath("$[0].moduleIcon").value("calculator"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                // Última aula do último módulo
                .andExpect(jsonPath("$[7].title").value("IPCA: como a inflação corrói seus ganhos"))
                .andExpect(jsonPath("$[7].videoId").value("LLANnZaSdQ0"))
                .andExpect(jsonPath("$[7].moduleTitle").value("Macroeconomia"))
                .andExpect(jsonPath("$[7].moduleIcon").value("globe"));
    }

    @Test
    @DisplayName("concluir uma aula marca só ela como concluída para o usuário")
    void concluirMarcaSoAAula() throws Exception {
        mockMvc.perform(post("/api/articles/{id}/complete", AULA_JUROS).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/articles").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.completed == true)].id", contains(AULA_JUROS)));
    }

    @Test
    @DisplayName("concluir de novo é idempotente: 204 e um único registro de progresso")
    void concluirDuasVezesEIdempotente() throws Exception {
        mockMvc.perform(post("/api/articles/{id}/complete", AULA_JUROS).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/articles/{id}/complete", AULA_JUROS).header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(progressRepository.findAllByIdUserId(ana.getId())).hasSize(1);
    }

    @Test
    @DisplayName("progresso é por usuário: a conclusão da Ana não aparece para a Bia")
    void progressoEPorUsuario() throws Exception {
        User bia = userRepository.save(User.builder()
                .username("bia").email("bia@retrobolsa.com").passwordHash("hash").build());
        mockMvc.perform(post("/api/articles/{id}/complete", AULA_JUROS).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/articles").header("Authorization", "Bearer " + jwtUtil.generateToken(bia.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].completed", everyItem(is(false))));
    }

    @Test
    @DisplayName("concluir aulas desbloqueia Estudante, depois Módulo Concluído e por fim Formado")
    void conclusoesDesbloqueiamConquistasDeAulas() throws Exception {
        concluir(AULA_RENTABILIDADE);
        assertThat(conquistasDe(ana)).containsExactly(AchievementCodes.PRIMEIRA_AULA);

        // Fecha o módulo 1 (3 aulas).
        concluir(AULA_JUROS);
        concluir("bbbbbbbb-0003-0000-0000-000000000003");
        assertThat(conquistasDe(ana)).containsExactlyInAnyOrder(
                AchievementCodes.PRIMEIRA_AULA, AchievementCodes.MODULO_COMPLETO);

        // Conclui as 5 restantes: todos os módulos fechados.
        for (int i = 4; i <= 8; i++) {
            concluir(String.format("bbbbbbbb-%04d-0000-0000-%012d", i, i));
        }
        assertThat(conquistasDe(ana)).containsExactlyInAnyOrder(
                AchievementCodes.PRIMEIRA_AULA, AchievementCodes.MODULO_COMPLETO, AchievementCodes.FORMADO);
    }

    private void concluir(String articleId) throws Exception {
        mockMvc.perform(post("/api/articles/{id}/complete", articleId).header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    private Set<String> conquistasDe(User usuario) {
        return achievementService.listForUser(usuario.getId()).stream()
                .filter(AchievementResponseDto::isUnlocked)
                .map(AchievementResponseDto::getCode)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("aula inexistente responde 400")
    void aulaInexistente() throws Exception {
        mockMvc.perform(post("/api/articles/{id}/complete", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Artigo nao encontrado")));
    }

    @Test
    @DisplayName("o banco recusa um video_id fora do formato do YouTube (CHECK do V10)")
    void bancoRecusaVideoIdInvalido() {
        // 11 caracteres de propósito: cabe no VARCHAR(11), então quem barra é a regex do CHECK.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE articles SET video_id = ? WHERE id = ?::uuid", "javascript:", AULA_RENTABILIDADE))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("video_id");
    }
}
