package com.retrobolsa.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.asset.AssetSnapshot;
import com.retrobolsa.api.game.asset.AssetSnapshotRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import com.retrobolsa.api.game.portfolio.AllocationRepository;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
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
import java.util.UUID;

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
    void deveRetornar400QuandoUsuarioNuncaSubmeteuCarteira() throws Exception {
        User usuario = criarUsuario("rafael@retrobolsa.com");

        mockMvc.perform(get("/api/portfolios/my-last-result")
                        .header("Authorization", "Bearer " + gerarToken(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Nenhum portfolio encontrado")));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Asset criarAssetComHistorico(String nomeAnonimo, BigDecimal retornoAnual) {
        Asset asset = assetRepository.save(Asset.builder()
                .anonymousName(nomeAnonimo)
                .realName("Empresa Real " + nomeAnonimo.replaceAll("\\D", ""))
                .type("stock")
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
        return userRepository.save(User.builder()
                .username("rafael")
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .build());
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