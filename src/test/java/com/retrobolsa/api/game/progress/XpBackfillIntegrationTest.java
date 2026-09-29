package com.retrobolsa.api.game.progress;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O V21 roda sobre dados de antes do XP. No teste, o Flyway o aplica num banco vazio
 * (no-op); aqui o cenário é montado direto no banco, sem os serviços (cujos gatilhos
 * dariam o XP sozinhos), e o arquivo da migration é executado de novo.
 */
class XpBackfillIntegrationTest extends AbstractIntegrationTest {

    private static final String AULA_1 = "bbbbbbbb-0001-0000-0000-000000000001";
    private static final String AULA_2 = "bbbbbbbb-0002-0000-0000-000000000002";
    private static final LocalDateTime FIM_DA_RODADA = LocalDateTime.of(2026, 2, 1, 12, 0);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    private UUID ana, beto, caio, admin, rodada;

    @BeforeEach
    void cenario() {
        jdbc.update("DELETE FROM xp_events");
        jdbc.update("DELETE FROM user_quiz_attempts");
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM portfolios");
        jdbc.update("DELETE FROM competition_assets");
        jdbc.update("DELETE FROM competitions");
        jdbc.update("DELETE FROM users");
        ana = usuario("ana", "PLAYER");
        beto = usuario("beto", "PLAYER");
        caio = usuario("caio", "PLAYER");
        admin = usuario("root", "ADMIN");

        // Rodada de 2020 a 2022: CDI de 7,31% e inflação de 15,03% no período (V14).
        rodada = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO competitions (id, round_number, status, budget, start_year, end_year, ends_at)
                VALUES (?, 1, 'revealed', 100000, 2020, 2022, ?)""", rodada, FIM_DA_RODADA);

        for (UUID quem : List.of(ana, admin)) {
            aula(quem, AULA_1, LocalDateTime.of(2026, 1, 1, 10, 0));
            aula(quem, AULA_2, LocalDateTime.of(2026, 1, 2, 10, 0));
            quiz(quem, AULA_1, 3, LocalDateTime.of(2026, 1, 1, 10, 0));
            quiz(quem, AULA_2, 2, LocalDateTime.of(2026, 1, 2, 10, 0));
            carteira(quem, "20.00");
            conquista(quem, "PRIMEIRA_AULA");
            conquista(quem, "PRIMEIRA_CARTEIRA");
        }
        carteira(beto, "5.00");

        // Caio fez as 20 aulas e já era Formado: 400 + 100 = 500 XP, nível 5.
        for (int i = 1; i <= 20; i++) {
            aula(caio, String.format("bbbbbbbb-%04d-0000-0000-%012d", i, i), LocalDateTime.of(2026, 3, i, 9, 0));
        }
        conquista(caio, "FORMADO");
    }

    private UUID usuario(String nome, String papel) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, username, email, password_hash, role) VALUES (?, ?, ?, 'hash', ?)",
                id, nome, nome + "@retrobolsa.com", papel);
        return id;
    }

    private void aula(UUID quem, String aula, LocalDateTime quando) {
        jdbc.update("INSERT INTO user_article_progress (user_id, article_id, completed_at) VALUES (?, ?::uuid, ?)", quem, aula, quando);
    }

    private void quiz(UUID quem, String aula, int nota, LocalDateTime quando) {
        jdbc.update("INSERT INTO user_quiz_attempts (user_id, article_id, score, total, created_at) VALUES (?, ?::uuid, ?, 3, ?)",
                quem, aula, nota, quando);
    }

    private void carteira(UUID quem, String rentabilidade) {
        jdbc.update("""
                INSERT INTO portfolios (user_id, competition_id, total_return, rank, submitted_at)
                VALUES (?, ?, ?::numeric, 1, ?)""", quem, rodada, rentabilidade, LocalDateTime.of(2026, 1, 15, 9, 0));
    }

    private void conquista(UUID quem, String codigo) {
        jdbc.update("""
                INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
                SELECT ?, id, ? FROM achievements WHERE code = ?""", quem, LocalDateTime.of(2026, 1, 1, 12, 0), codigo);
    }

    private void rodarBackfill() {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("db/migration/V21__backfill_xp.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    private int xp(UUID quem) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE user_id = ?", Integer.class, quem);
    }

    private List<String> conquistas(UUID quem) {
        return jdbc.queryForList("""
                SELECT a.code FROM user_achievements ua JOIN achievements a ON a.id = ua.achievement_id
                WHERE ua.user_id = ? ORDER BY a.code""", String.class, quem);
    }

    @Test
    void daOXpQueCadaUmJaTeriaEAsConquistasNovasDoHistorico() {
        rodarBackfill();

        // Ana: 2 aulas (40) + 1 quiz perfeito (15) + carteira (30) + acima do CDI (20)
        // + Estudante (10) + Primeira Carteira (10) + Nota Dez (10) + Venceu a Inflação (10) + Bateu o CDI (25).
        assertThat(xp(ana)).isEqualTo(170);
        assertThat(conquistas(ana)).containsExactly(
                "BATEU_CDI", "NOTA_DEZ", "PRIMEIRA_AULA", "PRIMEIRA_CARTEIRA", "VENCEU_INFLACAO");

        // Beto: 5% ficou abaixo do CDI e da inflação; só a carteira conta.
        assertThat(xp(beto)).isEqualTo(30);
        assertThat(conquistas(beto)).isEmpty();

        // Caio: 20 aulas (400) + Formado (100) = 500, o nível 5: ganha Analista e os 25 XP dela.
        assertThat(conquistas(caio)).containsExactly("FORMADO", "NIVEL_5");
        assertThat(xp(caio)).isEqualTo(525);

        // Admin não entra, como nos gatilhos.
        assertThat(xp(admin)).isZero();
        assertThat(conquistas(admin)).containsExactly("PRIMEIRA_AULA", "PRIMEIRA_CARTEIRA");
    }

    @Test
    void entraComoVistoENasDatasReais() {
        rodarBackfill();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xp_events WHERE NOT seen", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT created_at FROM xp_events WHERE user_id = ? AND source = 'BEAT_CDI'", LocalDateTime.class, ana))
                .isEqualTo(FIM_DA_RODADA);
        assertThat(jdbc.queryForObject(
                "SELECT created_at FROM xp_events WHERE user_id = ? AND source = 'LESSON' AND ref_id = ?",
                LocalDateTime.class, ana, AULA_2)).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
    }

    @Test
    void rodarDuasVezesNaoDuplicaNada() {
        rodarBackfill();
        int eventos = jdbc.queryForObject("SELECT COUNT(*) FROM xp_events", Integer.class);

        rodarBackfill();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xp_events", Integer.class)).isEqualTo(eventos);
        assertThat(xp(ana)).isEqualTo(170);
    }
}
