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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Notificações geradas pelos eventos de verdade: rodada aberta, resultado revelado e tarefa nova. */
class NotificationIntegrationTest extends AbstractIntegrationTest {

    private static final String RODADA_2011 = "dddddddd-0402-0000-0000-000000000402";
    private static final String AULA_1 = "bbbbbbbb-0001-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User admin;
    private User ana;
    private User beto;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM portfolios");
        jdbc.update("DELETE FROM classroom_members");
        jdbc.update("DELETE FROM classrooms");
        jdbc.update("UPDATE competitions SET status = 'draft'");
        userRepository.deleteAll();
        admin = criarUsuario("root", "ADMIN");
        ana = criarUsuario("ana", "PLAYER");
        beto = criarUsuario("beto", "PLAYER");
    }

    @Test
    @DisplayName("rodada aberta avisa todos os jogadores, e o resultado revelado só quem jogou")
    void rodadaEResultado() throws Exception {
        mockMvc.perform(post("/api/admin/competitions/" + RODADA_2011 + "/start").header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("RODADA_ABERTA"))
                .andExpect(jsonPath("$[0].title").value("Rodada 5 aberta"))
                .andExpect(jsonPath("$[0].link").value("/rodada/contexto"))
                .andExpect(jsonPath("$[0].read").value(false));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", empty()));

        // Só a Ana envia carteira; a rodada é simulada e revelada.
        String rodada = mockMvc.perform(get("/api/competitions/active")).andReturn().getResponse().getContentAsString();
        List<String> ativos = JsonPath.read(rodada, "$.assets[*].id");
        mockMvc.perform(post("/api/portfolios").header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":\"" + RODADA_2011 + "\",\"allocations\":[{\"assetId\":\"" + ativos.get(0)
                                + "\",\"amount\":100000}]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/competitions/" + RODADA_2011 + "/quick-simulate").header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$[*].type", contains("RESULTADO_REVELADO", "RODADA_ABERTA")))
                .andExpect(jsonPath("$[0].link").value("/rodada/resultado"));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$[*].type", contains("RODADA_ABERTA")));
    }

    @Test
    @DisplayName("tarefa nova avisa os alunos da turma, com o link da aula")
    void tarefaNova() throws Exception {
        User professora = criarUsuario("marta", "TEACHER");
        UUID turma = jdbc.queryForObject("""
                INSERT INTO classrooms (name, teacher_id, join_code) VALUES ('1º ano B', ?, 'ABCDEF') RETURNING id
                """, UUID.class, professora.getId());
        jdbc.update("INSERT INTO classroom_members (classroom_id, user_id) VALUES (?, ?)", turma, ana.getId());

        mockMvc.perform(post("/api/teacher/classrooms/" + turma + "/assignments").header("Authorization", bearer(professora))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":\"" + AULA_1 + "\",\"dueAt\":\"" + LocalDateTime.now().plusDays(2).withNano(0) + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$[0].type").value("TAREFA_NOVA"))
                .andExpect(jsonPath("$[0].title").value("Tarefa nova: O que é rentabilidade?"))
                .andExpect(jsonPath("$[0].body", startsWith("1º ano B · prazo ")))
                .andExpect(jsonPath("$[0].link", endsWith("/" + AULA_1)));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("contagem de não lidas, marcar lidas (só as próprias) e paginação")
    void lidasEPaginas() throws Exception {
        for (int i = 1; i <= 3; i++) {
            jdbc.update("INSERT INTO notifications (user_id, type, title, created_at) VALUES (?, 'RODADA_ABERTA', ?, NOW() - (? * INTERVAL '1 minute'))",
                    ana.getId(), "Aviso " + i, i);
        }
        jdbc.update("INSERT INTO notifications (user_id, type, title) VALUES (?, 'RODADA_ABERTA', 'Do Beto')", beto.getId());
        String doBeto = jdbc.queryForObject("SELECT id::text FROM notifications WHERE user_id = ?", String.class, beto.getId());

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.count").value(3));
        mockMvc.perform(get("/api/notifications").param("size", "2").param("page", "1").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$[*].title", contains("Aviso 3")));

        String maisNova = jdbc.queryForObject(
                "SELECT id::text FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 1", String.class, ana.getId());
        mockMvc.perform(post("/api/notifications/read").header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + maisNova + "\",\"" + doBeto + "\"]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.count").value(2));
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(post("/api/notifications/read").header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.count").value(0));
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    private User criarUsuario(String nome, String papel) {
        return userRepository.save(User.builder()
                .username(nome).email(nome + "@retrobolsa.com").passwordHash("hash").role(papel).build());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
