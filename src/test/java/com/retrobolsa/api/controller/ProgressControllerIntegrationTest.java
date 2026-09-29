package com.retrobolsa.api.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** XP, nível e comemoração pelo caminho real: aula por quiz, carteira enviada, "visto". */
class ProgressControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String AULA_RENTABILIDADE = "bbbbbbbb-0001-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private AssetSnapshotRepository snapshotRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM portfolios");
        jdbc.update("DELETE FROM competition_assets");
        jdbc.update("DELETE FROM competitions");
        userRepository.deleteAll();
        User ana = userRepository.save(User.builder().username("ana").email("ana@retrobolsa.com").passwordHash("hash").build());
        token = "Bearer " + jwtUtil.generateToken(ana.getEmail());
    }

    private JsonNode getJson(String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url).header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("exige login")
    void exigeLogin() throws Exception {
        mockMvc.perform(get("/api/progress")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress/news")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("jogador novo começa no nível 1, sem XP e sem novidades")
    void jogadorNovo() throws Exception {
        mockMvc.perform(get("/api/progress").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(0))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.levelTitle").value("Curioso"))
                .andExpect(jsonPath("$.nextLevelMinXp").value(50))
                .andExpect(jsonPath("$.streakWeeks").value(0));
        mockMvc.perform(get("/api/progress/news").header("Authorization", token))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.levelUp").value(false));
    }

    @Test
    @DisplayName("passar no quiz com nota máxima dá XP da aula, do quiz e das conquistas, e sobe de nível")
    void aulaPorQuiz() throws Exception {
        mockMvc.perform(post("/api/articles/{id}/quiz", AULA_RENTABILIDADE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(QuizRespostas.certas(jdbc, AULA_RENTABILIDADE)))
                .andExpect(status().isOk());

        // 20 (aula) + 15 (quiz perfeito) + 10 (Estudante) + 10 (Nota Dez) = 55: nível 2.
        mockMvc.perform(get("/api/progress").header("Authorization", token))
                .andExpect(jsonPath("$.xp").value(55))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.levelTitle").value("Aprendiz"))
                .andExpect(jsonPath("$.streakWeeks").value(1));

        JsonNode news = getJson("/api/progress/news");
        assertThat(news.get("xpGained").asInt()).isEqualTo(55);
        assertThat(news.get("levelUp").asBoolean()).isTrue();
        assertThat(news.get("levelTitle").asString()).isEqualTo("Aprendiz");
        assertThat(StreamSupport.stream(news.get("items").spliterator(), false).map(i -> i.get("label").asString()))
                .containsExactlyInAnyOrder(
                        "Aula concluída: O que é rentabilidade?",
                        "Nota máxima no quiz: O que é rentabilidade?",
                        "Conquista: Estudante",
                        "Conquista: Nota Dez");
        assertThat(StreamSupport.stream(news.get("achievements").spliterator(), false).map(a -> a.get("code").asString()))
                .containsExactlyInAnyOrder("PRIMEIRA_AULA", "NOTA_DEZ");

        // Refazer o quiz com nota máxima não dá XP de novo.
        mockMvc.perform(post("/api/articles/{id}/quiz", AULA_RENTABILIDADE).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(QuizRespostas.certas(jdbc, AULA_RENTABILIDADE)));
        mockMvc.perform(get("/api/progress").header("Authorization", token)).andExpect(jsonPath("$.xp").value(55));
    }

    @Test
    @DisplayName("marcar como visto tira da comemoração, sem mexer no XP; ids de outra pessoa são ignorados")
    void marcarComoVisto() throws Exception {
        mockMvc.perform(post("/api/articles/{id}/quiz", AULA_RENTABILIDADE).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(QuizRespostas.certas(jdbc, AULA_RENTABILIDADE)));
        JsonNode news = getJson("/api/progress/news");
        String ids = StreamSupport.stream(news.get("items").spliterator(), false)
                .map(i -> "\"" + i.get("id").asString() + "\"").reduce((a, b) -> a + "," + b).orElseThrow();

        // Outra pessoa tentando marcar os ganhos da Ana.
        User bia = userRepository.save(User.builder().username("bia").email("bia@retrobolsa.com").passwordHash("hash").build());
        mockMvc.perform(post("/api/progress/news/ack").header("Authorization", "Bearer " + jwtUtil.generateToken(bia.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + ids + "]}"))
                .andExpect(status().isNoContent());
        assertThat(getJson("/api/progress/news").get("items").size()).isEqualTo(4);

        mockMvc.perform(post("/api/progress/news/ack").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + ids + "]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/progress/news").header("Authorization", token))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.levelUp").value(false));
        mockMvc.perform(get("/api/progress").header("Authorization", token)).andExpect(jsonPath("$.xp").value(55));
    }

    @Test
    @DisplayName("enviar carteira dá XP e aparece com o número da rodada")
    void carteiraEnviada() throws Exception {
        Asset acao = assetRepository.save(Asset.builder().anonymousName("Empresa A").type("stock").build());
        snapshotRepository.save(AssetSnapshot.builder().asset(acao).year(2020).annualReturn(new BigDecimal("0.10")).build());
        Competition rodada = competitionRepository.save(Competition.builder()
                .roundNumber(3).status("open").budget(new BigDecimal("100000.00"))
                .startYear(2020).endYear(2021).endsAt(LocalDateTime.now().plusDays(3)).assets(List.of(acao)).build());

        mockMvc.perform(post("/api/portfolios").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":\"" + rodada.getId() + "\",\"allocations\":[{\"assetId\":\""
                                + acao.getId() + "\",\"amount\":100000}]}"))
                .andExpect(status().isCreated());

        JsonNode news = getJson("/api/progress/news");
        assertThat(StreamSupport.stream(news.get("items").spliterator(), false).map(i -> i.get("label").asString()))
                .contains("Carteira enviada na rodada 3", "Conquista: Primeira Carteira", "Conquista: Tudo Investido");
        // 30 (carteira) + 10 (Primeira Carteira) + 10 (Tudo Investido) = 50: nível 2.
        mockMvc.perform(get("/api/progress").header("Authorization", token))
                .andExpect(jsonPath("$.xp").value(50))
                .andExpect(jsonPath("$.level").value(2));
    }
}
