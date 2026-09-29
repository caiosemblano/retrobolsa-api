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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração de GET /api/competitions/active.
 */
class CompetitionControllerIntegrationTest extends AbstractIntegrationTest {

    private static final int START_YEAR = 2020;
    private static final int END_YEAR = 2023;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetSnapshotRepository snapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void limparBanco() {
        snapshotRepository.deleteAll();
        competitionRepository.deleteAll();
        assetRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void deveRetornarRodadaAtivaComIndicadoresApenasParaAcoes() throws Exception {
        Asset acao = criarAsset("Ação Anônima 1", "stock", "Financeiro");
        Asset titulo = criarAsset("Título Anônimo 1", "bond", null);

        criarSnapshot(acao, START_YEAR, new BigDecimal("12.50"), new BigDecimal("0.10"));
        criarSnapshot(titulo, START_YEAR, null, new BigDecimal("0.08"));

        criarCompeticaoAberta(1, List.of(acao, titulo), new BigDecimal("100000.00"));

        String token = gerarToken(criarUsuario("rafael@retrobolsa.com"));

        String responseBody = mockMvc.perform(get("/api/competitions/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(1))
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.assets", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        JsonNode assets = objectMapper.readTree(responseBody).get("assets");
        JsonNode assetAcao = buscarPorTipo(assets, "stock");
        JsonNode assetTitulo = buscarPorTipo(assets, "bond");

        assertThat(assetAcao.get("indicators").isNull()).isFalse();
        assertThat(assetAcao.get("indicators").get("pl").decimalValue()).isEqualByComparingTo("12.50");
        // ROE e Dividend Yield são ensinados nas aulas e agora aparecem no jogo.
        assertThat(assetAcao.get("indicators").get("roe").decimalValue()).isEqualByComparingTo("18.30");
        assertThat(assetAcao.get("indicators").get("dividendYield").decimalValue()).isEqualByComparingTo("4.20");

        assertThat(assetTitulo.get("indicators").isNull()).isTrue();
    }

    @Test
    void deveTrazerOCenarioEconomicoDoAnoAnteriorAoInicio() throws Exception {
        Asset acao = criarAsset("Ação Anônima 1", "stock", "Financeiro");
        criarCompeticaoAberta(1, List.of(acao), new BigDecimal("100000.00"));

        // Rodada de 2020 a 2023: quem investia no começo de 2020 conhecia os números de 2019.
        mockMvc.perform(get("/api/competitions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.economicIndicators[*].code", contains("SELIC", "IPCA", "DOLAR", "PIB")))
                .andExpect(jsonPath("$.economicIndicators[*].year", everyItem(is(START_YEAR - 1))))
                .andExpect(jsonPath("$.economicIndicators[0].value").value(4.5))
                .andExpect(jsonPath("$.economicIndicators[0].unit").value("% a.a."))
                .andExpect(jsonPath("$.economicIndicators[1].label").value("Inflação (IPCA)"))
                .andExpect(jsonPath("$.economicIndicators[1].value").value(4.31))
                .andExpect(jsonPath("$.economicIndicators[2].unit").value("R$"));
    }

    @Test
    void semDadosMacroParaOAnoOCenarioVemVazio() throws Exception {
        Asset acao = criarAsset("Ação Anônima 1", "stock", "Financeiro");
        competitionRepository.save(Competition.builder()
                .roundNumber(1).status("open").budget(new BigDecimal("100000.00"))
                .startYear(1980).endYear(1985).assets(List.of(acao)).build());

        mockMvc.perform(get("/api/competitions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.economicIndicators", hasSize(0)));
    }

    @Test
    void deveRetornar400QuandoNaoHaRodadaAberta() throws Exception {
        Asset acao = criarAsset("Ação Anônima 1", "stock", "Financeiro");
        criarCompeticaoFechada(1, List.of(acao));

        String token = gerarToken(criarUsuario("rafael@retrobolsa.com"));

        mockMvc.perform(get("/api/competitions/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("rodada ativa")));
    }

    @Test
    void devePermitirAcessoSemAutenticacao() throws Exception {
        // /api/competitions/active é publica (commit 66edfaa) para permitir que
        // visitantes nao autenticados vejam a rodada ativa antes de se cadastrar.
        Asset acao = criarAsset("Ação Anônima 1", "stock", "Financeiro");
        criarSnapshot(acao, START_YEAR, new BigDecimal("12.50"), new BigDecimal("0.10"));
        criarCompeticaoAberta(1, List.of(acao), new BigDecimal("100000.00"));

        mockMvc.perform(get("/api/competitions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(1));
    }

    @Test
    void deveRetornarAtivoSemIndicadoresQuandoNaoHaSnapshotNoAnoInicial() throws Exception {
        Asset acao = criarAsset("Ação Sem Snapshot", "stock", "Tecnologia");
        // nenhum snapshot criado para START_YEAR
        criarCompeticaoAberta(1, List.of(acao), new BigDecimal("100000.00"));

        String token = gerarToken(criarUsuario("rafael@retrobolsa.com"));

        String responseBody = mockMvc.perform(get("/api/competitions/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode asset = objectMapper.readTree(responseBody).get("assets").get(0);
        assertThat(asset.get("indicators").isNull()).isTrue();
        assertThat(asset.get("rate").isNull()).isTrue();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private JsonNode buscarPorTipo(JsonNode assets, String tipo) {
        Optional<JsonNode> encontrado = StreamSupport.stream(assets.spliterator(), false)
                .filter(node -> tipo.equals(node.get("type").asText()))
                .findFirst();
        assertThat(encontrado).as("asset do tipo '%s' presente na resposta", tipo).isPresent();
        return encontrado.get();
    }

    private Asset criarAsset(String nomeAnonimo, String tipo, String setor) {
        return assetRepository.save(Asset.builder()
                .anonymousName(nomeAnonimo)
                .type(tipo)
                .sector(setor)
                .build());
    }

    private AssetSnapshot criarSnapshot(Asset asset, int ano, BigDecimal pl, BigDecimal annualReturn) {
        return snapshotRepository.save(AssetSnapshot.builder()
                .asset(asset)
                .year(ano)
                .pl(pl)
                .roe(pl == null ? null : new BigDecimal("18.30"))
                .dividendYield(pl == null ? null : new BigDecimal("4.20"))
                .rate(annualReturn)
                .annualReturn(annualReturn)
                .build());
    }

    private Competition criarCompeticaoAberta(int round, List<Asset> assets, BigDecimal budget) {
        Competition competition = Competition.builder()
                .roundNumber(round)
                .status("open")
                .budget(budget)
                .startYear(START_YEAR)
                .endYear(END_YEAR)
                .assets(assets)
                .build();
        return competitionRepository.save(competition);
    }

    private Competition criarCompeticaoFechada(int round, List<Asset> assets) {
        Competition competition = Competition.builder()
                .roundNumber(round)
                .status("closed")
                .budget(new BigDecimal("100000.00"))
                .startYear(START_YEAR)
                .endYear(END_YEAR)
                .assets(assets)
                .build();
        return competitionRepository.save(competition);
    }

    private User criarUsuario(String email) {
        return userRepository.save(User.builder()
                .username("rafael")
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .build());
    }

    private String gerarToken(User user) {
        return jwtUtil.generateToken(user.getEmail());
    }
}