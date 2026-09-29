package com.retrobolsa.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.retrobolsa.api.game.achievement.AchievementCodes;
import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import com.retrobolsa.api.game.portfolio.AllocationRepository;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração de POST /api/portfolios e GET /api/portfolios/my-last-result.
 */
class PortfolioControllerIntegrationTest extends AbstractIntegrationTest {

    private static final int START_YEAR = 2020;
    private static final int END_YEAR = 2022;
    private static final BigDecimal BUDGET = new BigDecimal("100000.00");

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
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private PortfolioService portfolioService;

    @BeforeEach
    void limparBanco() {
        allocationRepository.deleteAll();
        portfolioRepository.deleteAll();
        snapshotRepository.deleteAll();
        competitionRepository.deleteAll();
        assetRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------------
    // POST /api/portfolios
    // ---------------------------------------------------------------

    @Test
    void deveSubmeterCarteiraComAlocacaoTotalDoOrcamento() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Asset ativo2 = criarAssetComHistorico("Ação 2", new BigDecimal("0.05"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1, ativo2));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("50000.00")),
                alocacao(ativo2.getId(), new BigDecimal("50000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Carteira submetida com sucesso"))
                .andExpect(jsonPath("$.warnings").doesNotExist());

        assertThat(portfolioRepository.findByUserIdAndCompetitionId(usuario.getId(), competicao.getId()))
                .isPresent();
    }

    @Test
    void deveAvisarQuandoAlocacaoForParcial() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("40000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasSize(1)))
                .andExpect(jsonPath("$.warnings[0]", containsString("ficaram parados em caixa")));
    }

    @Test
    void deveRejeitarQuandoTotalAlocadoExcedeOrcamento() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("150000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("excede o orcamento")));
    }

    @Test
    void deveRejeitarSubmissaoDuplicadaParaMesmaRodada() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");
        String token = gerarToken(usuario);

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("50000.00")));
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("ja submeteu")));
    }

    @Test
    void deveRejeitarQuandoRodadaNaoEncontrada() throws Exception {
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(UUID.randomUUID(),
                alocacao(UUID.randomUUID(), new BigDecimal("1000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Rodada nao encontrada")));
    }

    @Test
    void deveRejeitarQuandoRodadaNaoEstaAberta() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoFechada(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("50000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("nao esta aberta")));
    }

    @Test
    void deveRejeitarQuandoAtivoNaoPertenceARodada() throws Exception {
        Asset ativoDaRodada = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Asset ativoDeFora = criarAssetComHistorico("Ação Intrusa", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativoDaRodada));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativoDeFora.getId(), new BigDecimal("50000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("nao pertence a esta rodada")));
    }

    @Test
    void deveRejeitarQuandoAtivoDuplicadoNaMesmaSubmissao() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("20000.00")),
                alocacao(ativo1.getId(), new BigDecimal("20000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("mais de uma vez")));

        assertThat(portfolioRepository.findByUserIdAndCompetitionId(usuario.getId(), competicao.getId()))
                .isEmpty();
    }

    @Test
    void deveRejeitarQuandoValorAlocadoNaoEPositivo() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), BigDecimal.ZERO));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("deve ser positivo")));
    }

    @Test
    void deveRejeitarQuandoCamposObrigatoriosAusentes() throws Exception {
        User usuario = criarUsuario("rafael@retrobolsa.com");

        SubmitPortfolioRequestDto request = new SubmitPortfolioRequestDto();
        request.setCompetitionId(null);
        request.setAllocations(List.of());

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", not(empty())));
    }

    @Test
    void deveNegarSubmissaoSemAutenticacao() throws Exception {
        var request = requestComAlocacoes(UUID.randomUUID(),
                alocacao(UUID.randomUUID(), new BigDecimal("1000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // GET /api/portfolios/my-last-result
    // ---------------------------------------------------------------

    @Test
    void deveRetornarUltimoResultadoAposSubmissao() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");
        String token = gerarToken(usuario);

        var request = requestComAlocacoes(competicao.getId(),
                alocacao(ativo1.getId(), new BigDecimal("50000.00")));

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // O resultado só é exposto (com nomes reais) após a rodada ser encerrada e revelada.
        competicao.setStatus("revealed");
        competitionRepository.save(competicao);
        var portfolio = portfolioRepository.findByUserIdAndCompetitionId(usuario.getId(), competicao.getId())
                .orElseThrow();
        portfolio.setRank(1);
        portfolioRepository.save(portfolio);

        mockMvc.perform(get("/api/portfolios/my-last-result")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(1))
                .andExpect(jsonPath("$.period").value(START_YEAR + "-" + END_YEAR))
                .andExpect(jsonPath("$.revealedAssets", hasSize(1)))
                .andExpect(jsonPath("$.revealedAssets[0].realName").value("Empresa Real 1"));
    }

    @Test
    void resultadoRevelado_ensina_referencias_contribuicao_historiaEDicas() throws Exception {
        // 50 mil num ativo que rende 10% em 2020 e em 2021, e 50 mil parados:
        // o ativo vira 60.500 (+21%, somando 10,5 pontos) e a carteira rende 10,50%.
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        ativo1.setRevealNote("Frase sobre a empresa no período.");
        assetRepository.save(ativo1);
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");
        String token = gerarToken(usuario);
        submeter(usuario, competicao, alocacao(ativo1.getId(), new BigDecimal("50000.00")));
        encerrarComResultado(competicao, usuario, "revealed", "O que aconteceu de verdade em 2020 e 2021.");

        mockMvc.perform(get("/api/portfolios/my-last-result").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentability").value(10.5))
                // CDI e IPCA oficiais de 2020 e 2021 (V14): 2,75% e 4,44%; 4,52% e 10,06%.
                .andExpect(jsonPath("$.benchmarks[*].code", contains("CDI", "POUPANCA", "IBOVESPA", "IPCA")))
                .andExpect(jsonPath("$.benchmarks[0].totalReturn").value(7.31))
                .andExpect(jsonPath("$.benchmarks[3].totalReturn").value(15.03))
                .andExpect(jsonPath("$.benchmarks[0].chartData[*].year", contains(START_YEAR, START_YEAR + 1, END_YEAR)))
                .andExpect(jsonPath("$.revealedAssets[0].returnPct").value(21.0))
                .andExpect(jsonPath("$.revealedAssets[0].contribution").value(10.5))
                .andExpect(jsonPath("$.revealedAssets[0].revealNote").value("Frase sobre a empresa no período."))
                .andExpect(jsonPath("$.roundStats.participants").value(1))
                .andExpect(jsonPath("$.roundStats.medianReturn").value(10.5))
                .andExpect(jsonPath("$.roundStats.bestAsset.realName").value("Empresa Real 1"))
                .andExpect(jsonPath("$.roundStats.bestAsset.returnPct").value(21.0))
                .andExpect(jsonPath("$.debrief").value("O que aconteceu de verdade em 2020 e 2021."))
                .andExpect(jsonPath("$.tips[*].code",
                        contains("PERDEU_PARA_INFLACAO", "VENCEU_CDI", "CONCENTRADA", "DINHEIRO_PARADO")))
                // Com o CDI que aparece na tela (7,31%), para o aluno conseguir refazer a conta: 50.000 x 1,0731.
                .andExpect(jsonPath("$.tips[3].message")
                        .value("R$ 50.000,00 ficaram parados, rendendo 0%. No CDI, teriam virado R$ 53.655,00."));
    }

    @Test
    void resultadoSimuladoMasNaoRevelado_naoEntregaNomesNemAHistoria() throws Exception {
        Asset ativo1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        ativo1.setRevealNote("Frase que citaria a empresa.");
        assetRepository.save(ativo1);
        Competition competicao = criarCompeticaoAberta(1, List.of(ativo1));
        User usuario = criarUsuario("rafael@retrobolsa.com");
        submeter(usuario, competicao, alocacao(ativo1.getId(), new BigDecimal("100000.00")));
        encerrarComResultado(competicao, usuario, "simulated", "História que citaria a empresa.");

        mockMvc.perform(get("/api/portfolios/my-last-result").header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debrief").doesNotExist())
                .andExpect(jsonPath("$.revealedAssets[0].revealNote").doesNotExist())
                .andExpect(jsonPath("$.revealedAssets[0].returnPct").value(21.0))
                .andExpect(jsonPath("$.roundStats.bestAsset.anonymousName").value("Ação 1"))
                .andExpect(jsonPath("$.roundStats.bestAsset.realName").doesNotExist())
                .andExpect(jsonPath("$.benchmarks", hasSize(4)))
                .andExpect(jsonPath("$.tips", not(empty())));
    }

    /** Fecha a rodada como o admin faria, gravando a rentabilidade e a posição da carteira. */
    private void encerrarComResultado(Competition competicao, User usuario, String status, String debrief) {
        competicao.setStatus(status);
        competicao.setDebrief(debrief);
        competitionRepository.save(competicao);
        var portfolio = portfolioRepository.findByUserIdAndCompetitionId(usuario.getId(), competicao.getId()).orElseThrow();
        portfolio.setRank(1);
        portfolio.setTotalReturn(new BigDecimal("10.50"));
        portfolioRepository.save(portfolio);
    }

    @Test
    void deveRetornar400QuandoUsuarioNuncaSubmeteuCarteira() throws Exception {
        User usuario = criarUsuario("rafael@retrobolsa.com");

        mockMvc.perform(get("/api/portfolios/my-last-result")
                        .header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Nenhum portfolio encontrado")));
    }

    // ---------------------------------------------------------------
    // Conquistas desbloqueadas pelo fluxo real
    // ---------------------------------------------------------------

    @Test
    void deveDesbloquearConquistasDeMontagemAoSubmeterCarteiraCompleta() throws Exception {
        Asset acao1 = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Asset acao2 = criarAssetComHistorico("Ação 2", new BigDecimal("0.10"));
        Asset acao3 = criarAssetComHistorico("Ação 3", new BigDecimal("0.10"));
        Asset titulo1 = criarAssetComHistorico("Título 1", new BigDecimal("0.08"), "bond");
        Asset titulo2 = criarAssetComHistorico("Título 2", new BigDecimal("0.08"), "bond");
        Competition competicao = criarCompeticaoAberta(1, List.of(acao1, acao2, acao3, titulo1, titulo2));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        submeter(usuario, competicao,
                alocacao(acao1.getId(), new BigDecimal("20000.00")),
                alocacao(acao2.getId(), new BigDecimal("20000.00")),
                alocacao(acao3.getId(), new BigDecimal("20000.00")),
                alocacao(titulo1.getId(), new BigDecimal("20000.00")),
                alocacao(titulo2.getId(), new BigDecimal("20000.00")));

        assertThat(conquistasDe(usuario)).containsExactlyInAnyOrder(
                AchievementCodes.PRIMEIRA_CARTEIRA, AchievementCodes.TUDO_INVESTIDO,
                AchievementCodes.DIVERSIFICADOR, AchievementCodes.EQUILIBRISTA);
    }

    @Test
    void carteiraParcialSoDeAcoesGanhaSoPrimeiraCarteira() throws Exception {
        Asset acao = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(acao));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        submeter(usuario, competicao, alocacao(acao.getId(), new BigDecimal("40000.00")));

        assertThat(conquistasDe(usuario)).containsExactly(AchievementCodes.PRIMEIRA_CARTEIRA);
    }

    @Test
    void submissaoRecusadaNaoConcedeConquista() throws Exception {
        Asset acao = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Competition competicao = criarCompeticaoAberta(1, List.of(acao));
        User usuario = criarUsuario("rafael@retrobolsa.com");

        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestComAlocacoes(competicao.getId(),
                                alocacao(acao.getId(), new BigDecimal("150000.00"))))))
                .andExpect(status().isBadRequest());

        assertThat(conquistasDe(usuario)).isEmpty();
    }

    @Test
    void simulacaoDaRodadaConcedeConquistasDeResultado() throws Exception {
        // 10% a.a. por 2 anos = +21% ; -5% a.a. por 2 anos = -9,75%
        Asset vencedora = criarAssetComHistorico("Ação 1", new BigDecimal("0.10"));
        Asset perdedora = criarAssetComHistorico("Ação 2", new BigDecimal("-0.05"));
        Competition competicao = criarCompeticaoAberta(1, List.of(vencedora, perdedora));
        User ana = criarUsuario("ana", "ana@retrobolsa.com");
        User beto = criarUsuario("beto", "beto@retrobolsa.com");

        submeter(ana, competicao, alocacao(vencedora.getId(), BUDGET));
        submeter(beto, competicao, alocacao(perdedora.getId(), BUDGET));

        competicao.setStatus("closed");
        competitionRepository.save(competicao);
        portfolioService.simulateCompetition(competitionRepository.findById(competicao.getId()).orElseThrow());

        // Rodada de 2 jogadores: vale Campeão, mas não Pódio (exige 4).
        assertThat(conquistasDe(ana))
                .contains(AchievementCodes.CAMPEAO_RODADA, AchievementCodes.NO_AZUL, AchievementCodes.DOIS_DIGITOS)
                .doesNotContain(AchievementCodes.PODIO);
        assertThat(conquistasDe(beto))
                .doesNotContain(AchievementCodes.CAMPEAO_RODADA, AchievementCodes.NO_AZUL, AchievementCodes.DOIS_DIGITOS);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Asset criarAssetComHistorico(String nomeAnonimo, BigDecimal retornoAnual) {
        return criarAssetComHistorico(nomeAnonimo, retornoAnual, "stock");
    }

    private Asset criarAssetComHistorico(String nomeAnonimo, BigDecimal retornoAnual, String tipo) {
        Asset asset = assetRepository.save(Asset.builder()
                .anonymousName(nomeAnonimo)
                .realName("Empresa Real " + nomeAnonimo.replaceAll("\\D", ""))
                .type(tipo)
                .sector("Financeiro")
                .build());

        for (int ano = START_YEAR; ano < END_YEAR; ano++) {
            snapshotRepository.save(AssetSnapshot.builder()
                    .asset(asset)
                    .year(ano)
                    .annualReturn(retornoAnual)
                    .build());
        }
        return asset;
    }

    private Competition criarCompeticaoAberta(int round, List<Asset> assets) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(round)
                .status("open")
                .budget(BUDGET)
                .startYear(START_YEAR)
                .endYear(END_YEAR)
                .assets(assets)
                .build());
    }

    private Competition criarCompeticaoFechada(int round, List<Asset> assets) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(round)
                .status("closed")
                .budget(BUDGET)
                .startYear(START_YEAR)
                .endYear(END_YEAR)
                .assets(assets)
                .build());
    }

    private User criarUsuario(String email) {
        return criarUsuario("rafael", email);
    }

    private User criarUsuario(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .build());
    }

    private void submeter(User usuario, Competition competicao, SubmitPortfolioRequestDto.AllocationRequestDto... alocacoes)
            throws Exception {
        mockMvc.perform(post("/api/portfolios")
                        .header("Authorization", "Bearer " + gerarToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestComAlocacoes(competicao.getId(), alocacoes))))
                .andExpect(status().isCreated());
    }

    private Set<String> conquistasDe(User usuario) {
        return achievementService.listForUser(usuario.getId()).stream()
                .filter(AchievementResponseDto::isUnlocked)
                .map(AchievementResponseDto::getCode)
                .collect(Collectors.toSet());
    }

    private String gerarToken(User user) {
        return jwtUtil.generateToken(user.getEmail());
    }

    private SubmitPortfolioRequestDto.AllocationRequestDto alocacao(UUID assetId, BigDecimal amount) {
        SubmitPortfolioRequestDto.AllocationRequestDto dto = new SubmitPortfolioRequestDto.AllocationRequestDto();
        dto.setAssetId(assetId.toString());
        dto.setAmount(amount);
        return dto;
    }

    private SubmitPortfolioRequestDto requestComAlocacoes(UUID competitionId, SubmitPortfolioRequestDto.AllocationRequestDto... alocacoes) {
        SubmitPortfolioRequestDto request = new SubmitPortfolioRequestDto();
        request.setCompetitionId(competitionId.toString());
        request.setAllocations(List.of(alocacoes));
        return request;
    }
}