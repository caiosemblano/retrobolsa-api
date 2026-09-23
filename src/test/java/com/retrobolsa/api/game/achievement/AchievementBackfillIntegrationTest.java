package com.retrobolsa.api.game.achievement;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import com.retrobolsa.api.game.asset.Asset;
import com.retrobolsa.api.game.asset.AssetRepository;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.education.UserArticleProgress;
import com.retrobolsa.api.game.education.UserArticleProgressId;
import com.retrobolsa.api.game.education.UserArticleProgressRepository;
import com.retrobolsa.api.game.portfolio.Allocation;
import com.retrobolsa.api.game.portfolio.AllocationRepository;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.retrobolsa.api.game.achievement.AchievementCodes.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * O V11 roda sobre dados que já existiam antes das conquistas. No teste, o Flyway
 * o aplica num banco vazio (no-op), então aqui o cenário "pré-conquistas" é montado
 * direto pelos repositórios — sem passar pelos serviços, cujos gatilhos concederiam
 * as conquistas sozinhos — e o próprio arquivo de migration é executado de novo.
 */
class AchievementBackfillIntegrationTest extends AbstractIntegrationTest {

    private static final BigDecimal ORCAMENTO = new BigDecimal("100000.00");
    private static final LocalDateTime FIM_R1 = LocalDateTime.of(2026, 1, 10, 12, 0);

    @Autowired private DataSource dataSource;
    @Autowired private AchievementService achievementService;
    @Autowired private UserAchievementRepository userAchievementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private AllocationRepository allocationRepository;
    @Autowired private UserArticleProgressRepository progressRepository;

    private List<Asset> acoes;
    private List<Asset> titulos;

    @BeforeEach
    void limparBanco() {
        // Cascade leva carteiras, alocações, progresso e conquistas junto.
        userRepository.deleteAll();
        competitionRepository.deleteAll();
        acoes = List.of(ativo("stock"), ativo("stock"), ativo("stock"));
        titulos = List.of(ativo("bond"), ativo("bond"));
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private Asset ativo(String tipo) {
        return assetRepository.save(Asset.builder().anonymousName("Ativo " + UUID.randomUUID()).type(tipo).build());
    }

    private User usuario(String username, String role) {
        return userRepository.save(User.builder()
                .username(username).email(username + "@retrobolsa.com").passwordHash("hash").role(role).build());
    }

    private Competition rodada(int numero, String status, LocalDateTime fim) {
        return competitionRepository.save(Competition.builder()
                .roundNumber(numero).status(status).budget(ORCAMENTO)
                .startYear(2020).endYear(2022).endsAt(fim).build());
    }

    /** Carteira como ficava gravada antes das conquistas: rank/retorno só se a rodada foi simulada. */
    private void carteira(User user, Competition rodada, Integer rank, String retorno, LocalDateTime enviadaEm,
                          Map<Asset, String> alocacoes) {
        Portfolio portfolio = portfolioRepository.save(Portfolio.builder()
                .user(user).competition(rodada).rank(rank)
                .totalReturn(retorno == null ? null : new BigDecimal(retorno))
                .submittedAt(enviadaEm).build());
        alocacoes.forEach((asset, valor) -> allocationRepository.save(Allocation.builder()
                .portfolio(portfolio).asset(asset).amountInvested(new BigDecimal(valor))
                .percentWeight(new BigDecimal(valor).divide(ORCAMENTO)).build()));
    }

    private void concluiuAula(User user, int numeroDaAula, LocalDateTime quando) {
        UserArticleProgress progress = new UserArticleProgress();
        progress.setId(new UserArticleProgressId(user.getId(),
                UUID.fromString(String.format("bbbbbbbb-%04d-0000-0000-%012d", numeroDaAula, numeroDaAula))));
        progress.setCompletedAt(quando);
        progressRepository.save(progress);
    }

    private void rodarBackfill() {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("db/migration/V11__backfill_achievements.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    private Map<String, LocalDateTime> conquistasDe(User user) {
        return achievementService.listForUser(user.getId()).stream()
                .filter(AchievementResponseDto::isUnlocked)
                .collect(Collectors.toMap(AchievementResponseDto::getCode, AchievementResponseDto::getUnlockedAt));
    }

    // -------------------------------------------------------------------------
    // Cenário
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("concede a cada jogador exatamente o que ele já tinha feito, com a data real do feito")
    void concedeRetroativamente() {
        User ana = usuario("ana", "PLAYER");
        User beto = usuario("beto", "PLAYER");
        User caio = usuario("caio", "PLAYER");
        User duda = usuario("duda", "PLAYER");
        User admin = usuario("admin", "ADMIN");

        // R1: simulada, 4 jogadores — vale Campeão (≥2) e Pódio (≥4).
        Competition r1 = rodada(1, "simulated", FIM_R1);
        Asset acao = acoes.get(0);
        carteira(ana, r1, 1, "21.50", LocalDateTime.of(2026, 1, 2, 9, 0), Map.of(
                acoes.get(0), "20000.00", acoes.get(1), "20000.00", acoes.get(2), "20000.00",
                titulos.get(0), "20000.00", titulos.get(1), "20000.00"));
        carteira(beto, r1, 2, "12.00", LocalDateTime.of(2026, 1, 3, 9, 0), Map.of(acao, "40000.00"));
        carteira(caio, r1, 3, "3.00", LocalDateTime.of(2026, 1, 4, 9, 0), Map.of(acao, "100000.00"));
        carteira(duda, r1, 4, "-5.00", LocalDateTime.of(2026, 1, 5, 9, 0), Map.of(acao, "100000.00"));

        // R2: simulada, 1 jogador só — 1º lugar sozinho não é Campeão.
        Competition r2 = rodada(2, "simulated", LocalDateTime.of(2026, 2, 10, 12, 0));
        carteira(beto, r2, 1, "50.00", LocalDateTime.of(2026, 2, 2, 9, 0), Map.of(acao, "40000.00"));

        // R3: aberta, ainda sem simulação — não conta para conquistas de resultado.
        Competition r3 = rodada(3, "open", null);
        carteira(ana, r3, null, null, LocalDateTime.of(2026, 3, 1, 9, 0), Map.of(acao, "100000.00"));

        // Aulas: Ana fez todas; Beto fechou o módulo 1 (aulas 1-3); Caio fez uma; admin fez todas.
        for (int aula = 1; aula <= 8; aula++) {
            concluiuAula(ana, aula, LocalDateTime.of(2026, 1, aula, 20, 0));
            concluiuAula(admin, aula, LocalDateTime.of(2026, 1, aula, 21, 0));
        }
        concluiuAula(beto, 1, LocalDateTime.of(2026, 4, 1, 8, 0));
        concluiuAula(beto, 2, LocalDateTime.of(2026, 4, 2, 8, 0));
        concluiuAula(beto, 3, LocalDateTime.of(2026, 4, 3, 8, 0));
        concluiuAula(caio, 5, LocalDateTime.of(2026, 5, 1, 8, 0));

        assertThat(userAchievementRepository.count()).as("estado pré-conquistas").isZero();

        rodarBackfill();

        Map<String, LocalDateTime> daAna = conquistasDe(ana);
        assertThat(daAna.keySet()).containsExactlyInAnyOrder(
                PRIMEIRA_CARTEIRA, TUDO_INVESTIDO, DIVERSIFICADOR, EQUILIBRISTA,
                CAMPEAO_RODADA, PODIO, NO_AZUL, DOIS_DIGITOS,
                PRIMEIRA_AULA, MODULO_COMPLETO, FORMADO);
        // Datas reais: 1ª carteira (R1, não a da R3), fim da R1, 1ª e última aula.
        assertThat(daAna.get(PRIMEIRA_CARTEIRA)).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 0));
        assertThat(daAna.get(CAMPEAO_RODADA)).isEqualTo(FIM_R1);
        assertThat(daAna.get(PRIMEIRA_AULA)).isEqualTo(LocalDateTime.of(2026, 1, 1, 20, 0));
        assertThat(daAna.get(MODULO_COMPLETO)).isEqualTo(LocalDateTime.of(2026, 1, 3, 20, 0));
        assertThat(daAna.get(FORMADO)).isEqualTo(LocalDateTime.of(2026, 1, 8, 20, 0));

        assertThat(conquistasDe(beto).keySet()).containsExactlyInAnyOrder(
                PRIMEIRA_CARTEIRA, PODIO, NO_AZUL, DOIS_DIGITOS, PRIMEIRA_AULA, MODULO_COMPLETO);
        assertThat(conquistasDe(beto).get(MODULO_COMPLETO)).isEqualTo(LocalDateTime.of(2026, 4, 3, 8, 0));

        assertThat(conquistasDe(caio).keySet()).containsExactlyInAnyOrder(
                PRIMEIRA_CARTEIRA, TUDO_INVESTIDO, PODIO, NO_AZUL, PRIMEIRA_AULA);

        assertThat(conquistasDe(duda).keySet()).containsExactlyInAnyOrder(PRIMEIRA_CARTEIRA, TUDO_INVESTIDO);

        assertThat(conquistasDe(admin)).as("ADMIN não ganha conquista").isEmpty();
    }

    @Test
    @DisplayName("Veterano sai na 5ª carteira, com a data dela")
    void veteranoNaQuintaCarteira() {
        User eva = usuario("eva", "PLAYER");
        User fabio = usuario("fabio", "PLAYER");
        for (int rodada = 1; rodada <= 5; rodada++) {
            Competition r = rodada(rodada, "open", null);
            carteira(eva, r, null, null, LocalDateTime.of(2026, rodada, 1, 9, 0), Map.of(acoes.get(0), "1000.00"));
            if (rodada <= 4) {
                carteira(fabio, r, null, null, LocalDateTime.of(2026, rodada, 2, 9, 0), Map.of(acoes.get(0), "1000.00"));
            }
        }

        rodarBackfill();

        assertThat(conquistasDe(eva)).containsEntry(VETERANO, LocalDateTime.of(2026, 5, 1, 9, 0));
        assertThat(conquistasDe(fabio)).doesNotContainKey(VETERANO);
    }

    @Test
    @DisplayName("rodar de novo não duplica nem falha, e respeita o que já estava desbloqueado")
    void idempotente() {
        User ana = usuario("ana", "PLAYER");
        concluiuAula(ana, 1, LocalDateTime.of(2026, 1, 1, 20, 0));
        // Desbloqueada antes pelo gatilho, em outra data: o backfill não pode sobrescrever.
        achievementService.unlock(ana, List.of(PRIMEIRA_AULA));
        LocalDateTime dataOriginal = conquistasDe(ana).get(PRIMEIRA_AULA);

        rodarBackfill();
        rodarBackfill();

        assertThat(userAchievementRepository.findAllByIdUserId(ana.getId())).hasSize(1);
        assertThat(conquistasDe(ana).get(PRIMEIRA_AULA)).isEqualTo(dataOriginal);
    }
}
