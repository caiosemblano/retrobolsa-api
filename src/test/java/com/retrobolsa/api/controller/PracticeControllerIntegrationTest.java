package com.retrobolsa.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import com.retrobolsa.api.game.practice.PracticeRunRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Modo treino pelo caminho real: só rodadas reveladas, resultado do motor, nada no ranking e XP uma vez. */
class PracticeControllerIntegrationTest extends AbstractIntegrationTest {

    private static final int START_YEAR = 2020;
    private static final int END_YEAR = 2022;
    private static final BigDecimal BUDGET = new BigDecimal("100000.00");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private AssetSnapshotRepository snapshotRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private PortfolioService portfolioService;
    @Autowired private PracticeRunRepository practiceRunRepository;
    @Autowired private AchievementService achievementService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private JdbcTemplate jdbc;

    private User ana;
    private Asset acao;
    private Asset titulo;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM portfolios");
        jdbc.update("DELETE FROM practice_runs");
        jdbc.update("DELETE FROM competition_assets");
        jdbc.update("DELETE FROM competitions");
        userRepository.deleteAll();
        ana = criarUsuario("ana");
        // +10% ao ano e +5% ao ano, em 2020 e 2021.
        acao = criarAtivo("Ação Treino", "stock", new BigDecimal("0.10"));
        titulo = criarAtivo("Título Treino", "bond", new BigDecimal("0.05"));
    }

    @Test
    @DisplayName("exige login")
    void exigeLogin() throws Exception {
        mockMvc.perform(get("/api/practice")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("lista só as rodadas reveladas, com os treinos do jogador em cada uma")
    void listaSoRodadasReveladas() throws Exception {
        Competition revelada = criarRodada(1, "revealed");
        criarRodada(2, "open");
        criarRodada(3, "simulated");

        mockMvc.perform(get("/api/practice").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(revelada.getId().toString()))
                .andExpect(jsonPath("$[0].scenarioTitle").value("Cenário 1"))
                .andExpect(jsonPath("$[0].assetCount").value(2))
                .andExpect(jsonPath("$[0].runs").value(0))
                .andExpect(jsonPath("$[0].bestReturn").doesNotExist());

        treinar(ana, revelada, alocacao(acao, "100000.00")).andExpect(status().isOk());
        treinar(ana, revelada, alocacao(titulo, "100000.00")).andExpect(status().isOk());

        mockMvc.perform(get("/api/practice").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$[0].runs").value(2))
                .andExpect(jsonPath("$[0].bestReturn").value(21.0));
    }

    @Test
    @DisplayName("a montagem do treino mostra os ativos anônimos, sem o nome real")
    void montagemComAtivosAnonimos() throws Exception {
        Competition revelada = criarRodada(1, "revealed");

        mockMvc.perform(get("/api/practice/" + revelada.getId()).header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets", hasSize(2)))
                .andExpect(jsonPath("$.assets[*].anonymousName", containsInAnyOrder("Ação Treino", "Título Treino")))
                .andExpect(jsonPath("$.assets[0].realName").doesNotExist())
                .andExpect(jsonPath("$.economicIndicators", not(empty())));
    }

    @Test
    @DisplayName("recusa treinar numa rodada que ainda não foi revelada, para não entregar a resposta")
    void recusaRodadaNaoRevelada() throws Exception {
        Competition aberta = criarRodada(1, "open");
        Competition simulada = criarRodada(2, "simulated");

        treinar(ana, aberta, alocacao(acao, "100000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("reveladas")));
        treinar(ana, simulada, alocacao(acao, "100000.00")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/practice/" + aberta.getId()).header("Authorization", bearer(ana)))
                .andExpect(status().isBadRequest());

        assertThat(practiceRunRepository.count()).isZero();
    }

    @Test
    @DisplayName("o treino dá o mesmo resultado que a mesma carteira deu na rodada de verdade")
    void mesmoResultadoDoMotor() throws Exception {
        Competition rodada = criarRodada(1, "open");
        User beto = criarUsuario("beto");
        SubmitPortfolioRequestDto envio = new SubmitPortfolioRequestDto(rodada.getId().toString(),
                List.of(alocacao(acao, "60000.00"), alocacao(titulo, "40000.00")));
        mockMvc.perform(post("/api/portfolios").header("Authorization", bearer(beto))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(envio)))
                .andExpect(status().isCreated());
        rodada = competitionRepository.findById(rodada.getId()).orElseThrow();
        rodada.setStatus("closed");
        competitionRepository.save(rodada);
        portfolioService.simulateCompetition(competitionRepository.findById(rodada.getId()).orElseThrow());
        rodada = competitionRepository.findById(rodada.getId()).orElseThrow();
        rodada.setStatus("revealed");
        competitionRepository.save(rodada);

        // 60 mil x 1,21 + 40 mil x 1,1025 = 116.700: +16,70%.
        mockMvc.perform(get("/api/portfolios/my-last-result").header("Authorization", bearer(beto)))
                .andExpect(jsonPath("$.rentability").value(16.7));

        treinar(ana, rodada, alocacao(acao, "60000.00"), alocacao(titulo, "40000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(0))
                .andExpect(jsonPath("$.rentability").value(16.7))
                .andExpect(jsonPath("$.portfolioValue").value(116700.0))
                .andExpect(jsonPath("$.chartData", hasSize(3)))
                .andExpect(jsonPath("$.revealedAssets", hasSize(2)))
                .andExpect(jsonPath("$.revealedAssets[*].realName", containsInAnyOrder("Real Ação Treino", "Real Título Treino")))
                .andExpect(jsonPath("$.benchmarks", hasSize(4)))
                // As estatísticas são as da rodada de verdade: só o Beto jogou.
                .andExpect(jsonPath("$.roundStats.participants").value(1))
                .andExpect(jsonPath("$.roundStats.medianReturn").value(16.7))
                .andExpect(jsonPath("$.tips", not(empty())));
    }

    @Test
    @DisplayName("o treino não cria carteira, não dá pontos de ranking nem conquistas de jogo")
    void naoMexeNoJogo() throws Exception {
        Competition revelada = criarRodada(1, "revealed");

        treinar(ana, revelada, alocacao(acao, "50000.00"), alocacao(titulo, "50000.00"))
                .andExpect(status().isOk());

        assertThat(portfolioRepository.count()).isZero();
        assertThat(userRepository.findById(ana.getId()).orElseThrow().getTotalScore()).isZero();
        assertThat(achievementService.listForUser(ana.getId())).noneMatch(AchievementResponseDto::isUnlocked);
        assertThat(practiceRunRepository.countByUserId(ana.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("o XP do treino vem uma vez por rodada, por mais que o jogador treine de novo")
    void xpUmaVezPorRodada() throws Exception {
        Competition revelada = criarRodada(1, "revealed");

        treinar(ana, revelada, alocacao(acao, "100000.00")).andExpect(status().isOk());
        treinar(ana, revelada, alocacao(titulo, "100000.00")).andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT source, amount FROM xp_events WHERE user_id = ?", ana.getId()))
                .containsExactly(Map.of("source", "PRACTICE", "amount", 15));
        assertThat(practiceRunRepository.countByUserId(ana.getId())).isEqualTo(2);
        mockMvc.perform(get("/api/progress/news").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.items[0].label").value("Treino na rodada 1"));
    }

    @Test
    @DisplayName("valida a carteira de treino como o envio de verdade")
    void validaComoOEnvio() throws Exception {
        Competition revelada = criarRodada(1, "revealed");
        Asset deFora = criarAtivo("Ação de Fora", "stock", new BigDecimal("0.10"));

        treinar(ana, revelada, alocacao(deFora, "1000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("nao pertence a esta rodada")));
        treinar(ana, revelada, alocacao(acao, "150000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("excede o orcamento")));
        treinar(ana, revelada, alocacao(acao, "1000.00"), alocacao(acao, "1000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("mais de uma vez")));
        treinar(ana, revelada)
                .andExpect(status().isBadRequest());

        assertThat(practiceRunRepository.count()).isZero();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ResultActions treinar(User usuario, Competition rodada, SubmitPortfolioRequestDto.AllocationRequestDto... alocacoes)
            throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("allocations", List.of(alocacoes)));
        return mockMvc.perform(post("/api/practice/" + rodada.getId())
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private Competition criarRodada(int numero, String status) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(numero)
                .status(status)
                .budget(BUDGET)
                .scenarioTitle("Cenário " + numero)
                .startYear(START_YEAR)
                .endYear(END_YEAR)
                .assets(List.of(acao, titulo))
                .build());
    }

    private Asset criarAtivo(String nomeAnonimo, String tipo, BigDecimal retornoAnual) {
        Asset asset = assetRepository.save(Asset.builder()
                .anonymousName(nomeAnonimo)
                .realName("Real " + nomeAnonimo)
                .type(tipo)
                .sector("Teste")
                .build());
        for (int ano = START_YEAR; ano < END_YEAR; ano++) {
            snapshotRepository.save(AssetSnapshot.builder().asset(asset).year(ano).annualReturn(retornoAnual).build());
        }
        return asset;
    }

    private User criarUsuario(String nome) {
        return userRepository.save(User.builder()
                .username(nome).email(nome + "@retrobolsa.com").passwordHash("hash").build());
    }

    private String bearer(User usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario.getEmail());
    }

    private SubmitPortfolioRequestDto.AllocationRequestDto alocacao(Asset asset, String valor) {
        return new SubmitPortfolioRequestDto.AllocationRequestDto(asset.getId().toString(), new BigDecimal(valor));
    }
}
