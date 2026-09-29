package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ?turma= nos rankings: só os alunos da turma, com a posição recontada entre eles. */
class ClassroomRankingIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User professora;
    private User ana;
    private User zeca;
    private UUID turma;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM portfolios");
        jdbc.update("DELETE FROM competition_assets");
        jdbc.update("DELETE FROM competitions");
        jdbc.update("DELETE FROM classroom_members");
        jdbc.update("DELETE FROM classrooms");
        userRepository.deleteAll();

        professora = criarUsuario("marta", "TEACHER");
        ana = criarUsuario("ana", "PLAYER");
        User beto = criarUsuario("beto", "PLAYER");
        zeca = criarUsuario("zeca", "PLAYER");
        turma = jdbc.queryForObject("""
                INSERT INTO classrooms (name, teacher_id, join_code) VALUES ('1º ano B', ?, 'ABCDEF') RETURNING id
                """, UUID.class, professora.getId());
        jdbc.update("INSERT INTO classroom_members (classroom_id, user_id) VALUES (?, ?), (?, ?)",
                turma, ana.getId(), turma, beto.getId());

        // Na rodada, o Zeca (de fora da turma) ficou entre a Ana e o Beto.
        Competition rodada = criarRodada(1, "simulated");
        carteira(ana, rodada, 1, "20.00");
        carteira(zeca, rodada, 2, "10.00");
        carteira(beto, rodada, 3, "5.00");
    }

    @Test
    @DisplayName("o ranking da rodada filtrado pela turma reconta as posições entre os alunos")
    void rodadaDaTurma() throws Exception {
        mockMvc.perform(get("/api/rankings").param("type", "quinzenal").param("turma", turma.toString())
                        .header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("ana", "beto")))
                .andExpect(jsonPath("$[*].rank", contains(1, 2)));

        // Sem o filtro, continua o ranking de todos.
        mockMvc.perform(get("/api/rankings").param("type", "quinzenal"))
                .andExpect(jsonPath("$[*].username", contains("ana", "zeca", "beto")));
    }

    @Test
    @DisplayName("o professor vê a temporada da turma")
    void temporadaDaTurma() throws Exception {
        mockMvc.perform(get("/api/rankings").param("type", "season").param("turma", turma.toString())
                        .header("Authorization", bearer(professora)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("ana", "beto")))
                .andExpect(jsonPath("$[*].rank", contains(1, 2)));
    }

    @Test
    @DisplayName("na rodada aberta ninguém tem posição, nem no ranking da turma")
    void rodadaAberta() throws Exception {
        Competition aberta = criarRodada(2, "open");
        carteira(ana, aberta, null, null);

        mockMvc.perform(get("/api/rankings").param("type", "quinzenal").param("turma", turma.toString())
                        .header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$[*].username", contains("ana")))
                .andExpect(jsonPath("$[0].rank").value(0));
    }

    @Test
    @DisplayName("quem não é da turma, ou não entrou na conta, não vê o ranking dela")
    void acesso() throws Exception {
        mockMvc.perform(get("/api/rankings").param("type", "quinzenal").param("turma", turma.toString())
                        .header("Authorization", bearer(zeca)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro", containsString("professor e os alunos")));
        mockMvc.perform(get("/api/rankings").param("type", "quinzenal").param("turma", turma.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/rankings").param("type", "global").param("turma", turma.toString())
                        .header("Authorization", bearer(ana)))
                .andExpect(status().isBadRequest());
    }

    private Competition criarRodada(int numero, String status) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(numero).status(status).budget(new BigDecimal("100000.00"))
                .startYear(2020).endYear(2023).assets(List.of()).build());
    }

    private void carteira(User usuario, Competition rodada, Integer posicao, String rentabilidade) {
        portfolioRepository.save(Portfolio.builder()
                .user(usuario).competition(rodada).rank(posicao)
                .totalReturn(rentabilidade == null ? null : new BigDecimal(rentabilidade))
                .build());
    }

    private User criarUsuario(String nome, String papel) {
        return userRepository.save(User.builder()
                .username(nome).email(nome + "@retrobolsa.com").passwordHash("hash").role(papel).build());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }
}
