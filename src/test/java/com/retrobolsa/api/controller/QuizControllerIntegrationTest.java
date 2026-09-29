package com.retrobolsa.api.controller;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quizzes das aulas (V16) pelo caminho real: GET das perguntas e POST das respostas. */
class QuizControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String AULA_JUROS = "bbbbbbbb-0002-0000-0000-000000000002";
    private static final String P1 = "ffffffff-0002-0001-0000-000000000000";
    private static final String P1_CERTA = "ffffffff-0002-0001-0002-000000000000";
    private static final String P2 = "ffffffff-0002-0002-0000-000000000000";
    private static final String P3 = "ffffffff-0002-0003-0000-000000000000";
    private static final String P3_CERTA = "ffffffff-0002-0003-0004-000000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM user_quiz_attempts");
        userRepository.deleteAll();
        User ana = userRepository.save(User.builder()
                .username("ana").email("ana@retrobolsa.com").passwordHash("hash").build());
        token = "Bearer " + jwtUtil.generateToken(ana.getEmail());
    }

    private ResultActions responder(String articleId, String corpo) throws Exception {
        return mockMvc.perform(post("/api/articles/{id}/quiz", articleId).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions aulas() throws Exception {
        return mockMvc.perform(get("/api/articles").header("Authorization", token));
    }

    @Test
    @DisplayName("o seed tem 3 perguntas por aula, 4 alternativas cada e exatamente uma certa")
    void seedConsistente() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quiz_questions", Integer.class)).isEqualTo(24);
        assertThat(jdbc.queryForList("""
                SELECT article_id FROM quiz_questions GROUP BY article_id HAVING COUNT(*) <> 3
                """)).isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT q.id FROM quiz_questions q JOIN quiz_options o ON o.question_id = q.id
                GROUP BY q.id HAVING COUNT(*) <> 4 OR COUNT(*) FILTER (WHERE o.correct) <> 1
                """)).isEmpty();
    }

    @Test
    @DisplayName("GET devolve as perguntas na ordem, sem entregar a resposta nem a explicação")
    void perguntasSemGabarito() throws Exception {
        mockMvc.perform(get("/api/articles/{id}/quiz", AULA_JUROS).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(P1))
                .andExpect(jsonPath("$[0].prompt", startsWith("R$ 1.000 aplicados a 10% ao ano")))
                .andExpect(jsonPath("$[0].options", hasSize(4)))
                .andExpect(jsonPath("$[0].options[1].text").value("R$ 1.210"))
                .andExpect(jsonPath("$..correct").doesNotExist())
                .andExpect(jsonPath("$..explanation").doesNotExist());
    }

    @Test
    @DisplayName("tudo certo: 3 de 3, aula concluída e a correção explica cada pergunta")
    void tudoCerto() throws Exception {
        responder(AULA_JUROS, QuizRespostas.certas(jdbc, AULA_JUROS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(3))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.results[*].correct", everyItem(is(true))))
                .andExpect(jsonPath("$.results[0].correctOptionId").value(P1_CERTA))
                .andExpect(jsonPath("$.results[2].explanation", containsString("1,5 × 0,5 = 0,75")));

        aulas().andExpect(jsonPath("$[?(@.id == '" + AULA_JUROS + "')].completed", contains(true)))
                .andExpect(jsonPath("$[?(@.id == '" + AULA_JUROS + "')].bestQuizScore", contains(3)));
    }

    @Test
    @DisplayName("2 de 3 passa; 1 de 3 não conclui, mas a tentativa fica registrada")
    void notaMinima() throws Exception {
        responder(AULA_JUROS, QuizRespostas.comAcertos(jdbc, AULA_JUROS, 1))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.results[1].correct").value(false))
                .andExpect(jsonPath("$.results[1].selectedOptionId", not(equalTo(null))));
        aulas().andExpect(jsonPath("$[?(@.id == '" + AULA_JUROS + "')].completed", contains(false)))
                .andExpect(jsonPath("$[?(@.id == '" + AULA_JUROS + "')].bestQuizScore", contains(1)));

        responder(AULA_JUROS, QuizRespostas.comAcertos(jdbc, AULA_JUROS, 2))
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.passed").value(true));
        aulas().andExpect(jsonPath("$[?(@.id == '" + AULA_JUROS + "')].completed", contains(true)))
                .andExpect(jsonPath("$[?(@.id == '" + AULA_JUROS + "')].bestQuizScore", contains(2)));

        // Cada resposta de cada tentativa fica guardada (base para o painel do professor).
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_quiz_attempts", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_quiz_answers", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_quiz_answers WHERE question_id = ?::uuid AND NOT correct", Integer.class, P3))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("aula com quiz não se conclui pelo botão")
    void botaoRecusaAulaComQuiz() throws Exception {
        mockMvc.perform(post("/api/articles/{id}/complete", AULA_JUROS).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("se conclui pelo quiz")));
    }

    @Test
    @DisplayName("aula sem quiz: GET devolve lista vazia e o botão conclui")
    void aulaSemQuiz() throws Exception {
        String aula = "0b0b0b0b-0000-0000-0000-000000000001";
        jdbc.update("""
                INSERT INTO articles (id, module_id, title, duration_min, display_order)
                VALUES (?::uuid, 'aaaaaaaa-0001-0000-0000-000000000001', 'Aula sem quiz', 3, 99)
                """, aula);
        try {
            mockMvc.perform(get("/api/articles/{id}/quiz", aula).header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
            mockMvc.perform(post("/api/articles/{id}/complete", aula).header("Authorization", token))
                    .andExpect(status().isNoContent());
            aulas().andExpect(jsonPath("$[?(@.id == '" + aula + "')].hasQuiz", contains(false)))
                    .andExpect(jsonPath("$[?(@.id == '" + aula + "')].completed", contains(true)));
            responder(aula, QuizRespostas.de(List.<String[]>of(new String[]{P1, P1_CERTA})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro", containsString("nao tem quiz")));
        } finally {
            jdbc.update("DELETE FROM articles WHERE id = ?::uuid", aula);
        }
    }

    @Test
    @DisplayName("respostas incompletas, repetidas ou de outra pergunta são recusadas sem registrar nada")
    void respostasInvalidas() throws Exception {
        responder(AULA_JUROS, QuizRespostas.de(List.of(new String[]{P1, P1_CERTA}, new String[]{P3, P3_CERTA})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Responda todas")));
        responder(AULA_JUROS, QuizRespostas.de(List.of(
                        new String[]{P1, P1_CERTA}, new String[]{P1, P1_CERTA}, new String[]{P3, P3_CERTA})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("mais de uma vez")));
        // Alternativa certa da pergunta 3 dada como resposta da pergunta 2.
        responder(AULA_JUROS, QuizRespostas.de(List.of(
                        new String[]{P1, P1_CERTA}, new String[]{P2, P3_CERTA}, new String[]{P3, P3_CERTA})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Alternativa invalida")));
        responder(AULA_JUROS, "{\"answers\":[]}").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_quiz_attempts", Integer.class)).isZero();
    }

    @Test
    @DisplayName("aula inexistente responde 400, e o quiz exige login")
    void aulaInexistenteESemLogin() throws Exception {
        mockMvc.perform(get("/api/articles/{id}/quiz", "00000000-0000-0000-0000-000000000000").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Artigo nao encontrado")));
        mockMvc.perform(get("/api/articles/{id}/quiz", AULA_JUROS)).andExpect(status().isUnauthorized());
    }
}
