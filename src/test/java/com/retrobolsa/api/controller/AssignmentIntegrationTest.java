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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tarefas da turma: o professor passa aulas com prazo, acompanha, e o aluno vê o que falta. */
class AssignmentIntegrationTest extends AbstractIntegrationTest {

    private static final String AULA_1 = "bbbbbbbb-0001-0000-0000-000000000001";
    private static final String AULA_2 = "bbbbbbbb-0002-0000-0000-000000000002";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User professora;
    private User ana;
    private User beto;
    private UUID turma;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM classroom_members");
        jdbc.update("DELETE FROM classrooms");
        userRepository.deleteAll();
        professora = criarUsuario("marta", "TEACHER");
        ana = criarUsuario("ana", "PLAYER");
        beto = criarUsuario("beto", "PLAYER");
        turma = jdbc.queryForObject("""
                INSERT INTO classrooms (name, teacher_id, join_code) VALUES ('1º ano B', ?, 'ABCDEF') RETURNING id
                """, UUID.class, professora.getId());
        jdbc.update("INSERT INTO classroom_members (classroom_id, user_id) VALUES (?, ?), (?, ?)",
                turma, ana.getId(), turma, beto.getId());
    }

    @Test
    @DisplayName("o professor passa uma aula e vê quem fez; o aluno vê só o que falta")
    void acompanhamento() throws Exception {
        passar(AULA_1, amanha())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articleTitle").isNotEmpty())
                .andExpect(jsonPath("$.pending").value(2));

        // A Ana concluiu a aula; o Beto ainda não.
        jdbc.update("INSERT INTO user_article_progress (user_id, article_id) VALUES (?, ?::uuid)", ana.getId(), AULA_1);

        mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/assignments").header("Authorization", bearer(professora)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].done").value(1))
                .andExpect(jsonPath("$[0].pending").value(1))
                .andExpect(jsonPath("$[0].students[*].username", contains("ana", "beto")))
                .andExpect(jsonPath("$[0].students[*].status", contains("FEITA", "PENDENTE")))
                .andExpect(jsonPath("$[0].students[0].completedAt").isNotEmpty());

        mockMvc.perform(get("/api/classrooms/assignments").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$", empty()));
        mockMvc.perform(get("/api/classrooms/assignments").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].classroomName").value("1º ano B"))
                .andExpect(jsonPath("$[0].articleId").value(AULA_1))
                .andExpect(jsonPath("$[0].late").value(false));
    }

    @Test
    @DisplayName("passado o prazo, quem não fez aparece como atrasado")
    void atrasada() throws Exception {
        passar(AULA_2, amanha()).andExpect(status().isCreated());
        jdbc.update("UPDATE classroom_assignments SET due_at = NOW() - INTERVAL '1 day'");

        mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/assignments").header("Authorization", bearer(professora)))
                .andExpect(jsonPath("$[0].late").value(2))
                .andExpect(jsonPath("$[0].students[*].status", everyItem(is("ATRASADA"))));
        mockMvc.perform(get("/api/classrooms/assignments").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$[0].late").value(true));
    }

    @Test
    @DisplayName("prazo no passado, aula repetida ou inexistente são recusados")
    void recusas() throws Exception {
        passar(AULA_1, LocalDateTime.now().minusHours(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("depois de agora")));
        passar(UUID.randomUUID().toString(), amanha()).andExpect(status().isBadRequest());
        passar(AULA_1, amanha()).andExpect(status().isCreated());
        passar(AULA_1, amanha())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("já é uma tarefa")));
    }

    @Test
    @DisplayName("outro professor não vê nem apaga as tarefas, aluno não passa tarefa, e o professor apaga a dele")
    void autorizacaoEApagar() throws Exception {
        String id = com.jayway.jsonpath.JsonPath.read(passar(AULA_1, amanha()).andReturn().getResponse().getContentAsString(), "$.id");
        User outro = criarUsuario("paulo", "TEACHER");

        mockMvc.perform(get("/api/teacher/classrooms/" + turma + "/assignments").header("Authorization", bearer(outro)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/teacher/classrooms/" + turma + "/assignments/" + id).header("Authorization", bearer(outro)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/teacher/classrooms/" + turma + "/assignments").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/teacher/classrooms/" + turma + "/assignments/" + id).header("Authorization", bearer(professora)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/classrooms/assignments").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$", empty()));
    }

    private ResultActions passar(String aula, String prazo) throws Exception {
        return mockMvc.perform(post("/api/teacher/classrooms/" + turma + "/assignments")
                .header("Authorization", bearer(professora))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"articleId\":\"" + aula + "\",\"dueAt\":\"" + prazo + "\"}"));
    }

    private static String amanha() {
        return LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private User criarUsuario(String nome, String papel) {
        return userRepository.save(User.builder()
                .username(nome).email(nome + "@retrobolsa.com").passwordHash("hash").role(papel).build());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
