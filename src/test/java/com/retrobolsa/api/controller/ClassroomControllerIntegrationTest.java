package com.retrobolsa.api.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Turmas pelo caminho real: o admin promove, o professor cria, o aluno entra e sai. */
class ClassroomControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User admin;
    private User professora;
    private User aluno;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM classroom_members");
        jdbc.update("DELETE FROM classrooms");
        userRepository.deleteAll();
        admin = criarUsuario("root", "ADMIN");
        professora = criarUsuario("marta", "TEACHER");
        aluno = criarUsuario("joao", "PLAYER");
    }

    // ---------------------------------------------------------------
    // Admin: promover a professor
    // ---------------------------------------------------------------

    @Test
    @DisplayName("o admin busca um usuário e o promove a professor; sem busca, lista os professores")
    void adminPromoveAProfessor() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("q", "JOA").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("joao"))
                .andExpect(jsonPath("$[0].role").value("PLAYER"));

        mockMvc.perform(post("/api/admin/users/" + aluno.getId() + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[*].username", contains("joao", "marta")));
        // O papel vem do banco a cada requisição: a promoção vale na hora, sem novo login.
        mockMvc.perform(get("/api/teacher/classrooms").header("Authorization", bearer(aluno)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("o admin não cria outro admin nem muda o papel de um admin, e só ele promove")
    void limitesDaPromocao() throws Exception {
        mudarPapel(admin, aluno, "ADMIN").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("PLAYER ou TEACHER")));
        mudarPapel(admin, admin, "PLAYER").andExpect(status().isBadRequest());
        mudarPapel(professora, aluno, "TEACHER").andExpect(status().isForbidden());
        assertThat(userRepository.findById(aluno.getId()).orElseThrow().getRole()).isEqualTo("PLAYER");
    }

    // ---------------------------------------------------------------
    // Professor
    // ---------------------------------------------------------------

    @Test
    @DisplayName("o professor cria a turma e recebe um código de 6 caracteres sem 0, O, 1 nem I")
    void professorCriaTurma() throws Exception {
        JsonNode turma = criarTurma(professora, "1º ano B");

        assertThat(turma.get("joinCode").asText()).matches("[A-HJ-NP-Z2-9]{6}");
        assertThat(turma.get("institution").asText()).isEqualTo("Escola Estadual");
        mockMvc.perform(get("/api/teacher/classrooms").header("Authorization", bearer(professora)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("1º ano B"))
                .andExpect(jsonPath("$[0].memberCount").value(0));
    }

    @Test
    @DisplayName("aluno não entra na área do professor, e um professor não mexe na turma de outro")
    void autorizacao() throws Exception {
        mockMvc.perform(get("/api/teacher/classrooms").header("Authorization", bearer(aluno)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teacher/classrooms")).andExpect(status().isUnauthorized());

        String turmaDaMarta = criarTurma(professora, "1º ano B").get("id").asText();
        User outro = criarUsuario("paulo", "TEACHER");
        mockMvc.perform(post("/api/teacher/classrooms/" + turmaDaMarta + "/archive").header("Authorization", bearer(outro)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Turma não encontrada"));
        mockMvc.perform(get("/api/teacher/classrooms").header("Authorization", bearer(outro)))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("um código novo invalida o antigo")
    void novoCodigo() throws Exception {
        JsonNode turma = criarTurma(professora, "1º ano B");
        String antigo = turma.get("joinCode").asText();

        String novo = objectMapper.readTree(mockMvc.perform(post("/api/teacher/classrooms/" + turma.get("id").asText() + "/code")
                        .header("Authorization", bearer(professora)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("joinCode").asText();

        assertThat(novo).isNotEqualTo(antigo);
        entrar(aluno, antigo).andExpect(status().isBadRequest());
        entrar(aluno, novo).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Aluno
    // ---------------------------------------------------------------

    @Test
    @DisplayName("o aluno entra com o código (mesmo em minúsculas e com espaço), vê a turma e sai quando quiser")
    void alunoEntraESai() throws Exception {
        JsonNode turma = criarTurma(professora, "1º ano B");
        String codigo = turma.get("joinCode").asText();
        String digitado = " " + codigo.substring(0, 3).toLowerCase() + " " + codigo.substring(3).toLowerCase();

        entrar(aluno, digitado)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("1º ano B"))
                .andExpect(jsonPath("$.teacherUsername").value("marta"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.joinCode").doesNotExist());
        // Entrar de novo não duplica.
        entrar(aluno, codigo).andExpect(status().isOk()).andExpect(jsonPath("$.memberCount").value(1));

        mockMvc.perform(get("/api/classrooms/mine").header("Authorization", bearer(aluno)))
                .andExpect(jsonPath("$", hasSize(1)));

        String id = turma.get("id").asText();
        mockMvc.perform(delete("/api/classrooms/" + id + "/membership").header("Authorization", bearer(aluno)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/classrooms/mine").header("Authorization", bearer(aluno)))
                .andExpect(jsonPath("$", empty()));
        mockMvc.perform(delete("/api/classrooms/" + id + "/membership").header("Authorization", bearer(aluno)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("código inválido, turma arquivada e o próprio professor não entram")
    void recusasAoEntrar() throws Exception {
        entrar(aluno, "ZZZZZZ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Código de turma inválido")));
        entrar(aluno, "").andExpect(status().isBadRequest());

        JsonNode turma = criarTurma(professora, "1º ano B");
        String codigo = turma.get("joinCode").asText();
        entrar(professora, codigo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("professor desta turma")));

        mockMvc.perform(post("/api/teacher/classrooms/" + turma.get("id").asText() + "/archive")
                        .header("Authorization", bearer(professora)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
        entrar(aluno, codigo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("arquivada")));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private JsonNode criarTurma(User professor, String nome) throws Exception {
        String corpo = objectMapper.writeValueAsString(java.util.Map.of("name", nome, "institution", "Escola Estadual"));
        return objectMapper.readTree(mockMvc.perform(post("/api/teacher/classrooms")
                        .header("Authorization", bearer(professor))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions entrar(User usuario, String codigo) throws Exception {
        return mockMvc.perform(post("/api/classrooms/join").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("code", codigo))));
    }

    private ResultActions mudarPapel(User quem, User alvo, String papel) throws Exception {
        return mockMvc.perform(post("/api/admin/users/" + alvo.getId() + "/role").header("Authorization", bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"" + papel + "\"}"));
    }

    private User criarUsuario(String nome, String papel) {
        return userRepository.save(User.builder()
                .username(nome).email(nome + "@retrobolsa.com").passwordHash("hash").role(papel).build());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
