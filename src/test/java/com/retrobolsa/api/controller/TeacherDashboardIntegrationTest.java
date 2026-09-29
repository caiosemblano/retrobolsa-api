package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.progress.Levels;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * O painel do professor com dados de verdade no banco: dois alunos na turma e
 * uma aluna de fora, cujas respostas não podem entrar nas estatísticas.
 */
class TeacherDashboardIntegrationTest extends AbstractIntegrationTest {

    private static final String AULA_1 = "bbbbbbbb-0001-0000-0000-000000000001";
    private static final String AULA_2 = "bbbbbbbb-0002-0000-0000-000000000002";
    /** Aula 2: juros compostos (R$ 1.210), diferença entre simples e compostos, +50% e −50%. */
    private static final String Q2_1 = "ffffffff-0002-0001-0000-000000000000";
    private static final String Q2_2 = "ffffffff-0002-0002-0000-000000000000";
    private static final String Q2_3 = "ffffffff-0002-0003-0000-000000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User professora;
    private UUID turma;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM classroom_members");
        jdbc.update("DELETE FROM classrooms");
        userRepository.deleteAll();
        professora = criarUsuario("marta", "TEACHER");
        User ana = criarUsuario("ana", "PLAYER");
        User beto = criarUsuario("Beto", "PLAYER");
        User carla = criarUsuario("carla", "PLAYER");

        turma = jdbc.queryForObject("""
                INSERT INTO classrooms (name, teacher_id, join_code) VALUES ('1º ano B', ?, 'ABCDEF') RETURNING id
                """, UUID.class, professora.getId());
        jdbc.update("INSERT INTO classroom_members (classroom_id, user_id) VALUES (?, ?), (?, ?)",
                turma, ana.getId(), turma, beto.getId());

        // Ana: concluiu a aula 1 com nota máxima e errou duas da aula 2.
        jdbc.update("INSERT INTO user_article_progress (user_id, article_id) VALUES (?, ?::uuid)", ana.getId(), AULA_1);
        tentativa(ana, AULA_1, 3, opcao("0001-0001", 2), opcao("0001-0002", 3), opcao("0001-0003", 1));
        tentativa(ana, AULA_2, 1, opcao("0002-0001", 1), opcao("0002-0002", 1), opcao("0002-0003", 3));
        jdbc.update("INSERT INTO xp_events (user_id, source, ref_id, amount) VALUES (?, 'LESSON', ?, 30)", ana.getId(), AULA_1);
        // Beto: errou a primeira e a segunda da aula 2.
        tentativa(beto, AULA_2, 1, opcao("0002-0001", 1), opcao("0002-0002", 2), opcao("0002-0003", 4));
        // Carla está fora da turma: errou tudo, e nada disso pode aparecer.
        tentativa(carla, AULA_1, 0, opcao("0001-0001", 1), opcao("0001-0002", 1), opcao("0001-0003", 2));
    }

    @Test
    @DisplayName("a tabela de alunos traz aulas, quizzes, XP e última atividade, só dos alunos da turma")
    void alunosDaTurma() throws Exception {
        Levels.Level nivelDaAna = Levels.of(30);
        mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/students").header("Authorization", bearer(professora)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("ana", "Beto")))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].lessonsCompleted").value(1))
                .andExpect(jsonPath("$[0].quizzesTaken").value(2))
                // A melhor nota de cada quiz: 100% na aula 1 e 33,3% na aula 2.
                .andExpect(jsonPath("$[0].quizAverage").value(66.7))
                .andExpect(jsonPath("$[0].xp").value(30))
                .andExpect(jsonPath("$[0].level").value(nivelDaAna.number()))
                .andExpect(jsonPath("$[0].lastActivity").isNotEmpty())
                .andExpect(jsonPath("$[1].lessonsCompleted").value(0))
                .andExpect(jsonPath("$[1].quizAverage").value(33.3))
                .andExpect(jsonPath("$[1].roundsPlayed").value(0));
    }

    @Test
    @DisplayName("as perguntas mais erradas vêm pela taxa de erro, com a alternativa errada mais escolhida")
    void perguntasMaisErradas() throws Exception {
        mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/questions").header("Authorization", bearer(professora)))
                .andExpect(status().isOk())
                // As da aula 1 não aparecem: na turma, ninguém errou (a Carla, de fora, não conta).
                .andExpect(jsonPath("$[*].questionId", contains(Q2_1, Q2_2, Q2_3)))
                .andExpect(jsonPath("$[0].errorRate").value(100.0))
                .andExpect(jsonPath("$[0].answers").value(2))
                .andExpect(jsonPath("$[0].students").value(2))
                .andExpect(jsonPath("$[0].commonWrongAnswer").value("R$ 1.200"))
                .andExpect(jsonPath("$[0].articleTitle").isNotEmpty())
                .andExpect(jsonPath("$[1].errorRate").value(50.0))
                .andExpect(jsonPath("$[1].commonWrongAnswer").value("Os juros simples sempre rendem mais"));
    }

    @Test
    @DisplayName("o CSV abre certo numa planilha em português")
    void csv() throws Exception {
        var resposta = mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/students.csv")
                        .header("Authorization", bearer(professora)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("turma-1o-ano-b.csv")))
                .andReturn().getResponse();

        String[] linhas = new String(resposta.getContentAsByteArray(), StandardCharsets.UTF_8).split("\r\n");
        assertThat(linhas[0]).startsWith("﻿Aluno;Aulas concluídas;");
        assertThat(linhas).hasSize(3);
        assertThat(linhas[1]).startsWith("ana;1;2;66,7;0;30;" + Levels.of(30).number() + " - ");
        assertThat(linhas[2]).startsWith("Beto;0;1;33,3;0;0;");
    }

    @Test
    @DisplayName("outro professor não vê nada da turma, e aluno não entra no painel")
    void autorizacao() throws Exception {
        User outro = criarUsuario("paulo", "TEACHER");
        for (String caminho : new String[]{"/students", "/questions", "/students.csv"}) {
            mockMvc.perform(get("/api/teacher/classrooms/" + turma + caminho).header("Authorization", bearer(outro)))
                    .andExpect(status().isNotFound());
        }
        User ana = userRepository.findByEmail("ana@retrobolsa.com").orElseThrow();
        mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/students").header("Authorization", bearer(ana)))
                .andExpect(status().isForbidden());
    }

    private void tentativa(User usuario, String aula, int acertos, String... opcoes) {
        UUID tentativa = jdbc.queryForObject("""
                INSERT INTO user_quiz_attempts (user_id, article_id, score, total) VALUES (?, ?::uuid, ?, 3) RETURNING id
                """, UUID.class, usuario.getId(), aula, acertos);
        for (String opcao : opcoes) {
            jdbc.update("""
                    INSERT INTO user_quiz_answers (attempt_id, question_id, option_id, correct)
                    SELECT ?, o.question_id, o.id, o.correct FROM quiz_options o WHERE o.id = ?::uuid
                    """, tentativa, opcao);
        }
    }

    /** "0002-0001", 3 → a 3ª alternativa da 1ª pergunta da aula 2. */
    private static String opcao(String aulaEPergunta, int alternativa) {
        return "ffffffff-" + aulaEPergunta + "-000" + alternativa + "-000000000000";
    }

    private User criarUsuario(String nome, String papel) {
        return userRepository.save(User.builder()
                .username(nome).email(nome.toLowerCase() + "@retrobolsa.com").passwordHash("hash").role(papel).build());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
