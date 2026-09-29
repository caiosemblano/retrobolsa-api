package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A V15 dá a cada rodada do seed o "o que aconteceu" e a cada ativo a sua frase de revelação. */
class DebriefMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void todaRodadaDoSeedTemOQueAconteceu() {
        List<Map<String, Object>> rodadas = jdbc.queryForList(
                "SELECT scenario_title, start_year, debrief FROM competitions ORDER BY start_year");

        assertThat(rodadas).extracting(r -> r.get("start_year")).containsExactly(2004, 2011, 2014, 2017, 2020);
        assertThat(rodadas).allSatisfy(r -> assertThat((String) r.get("debrief"))
                .as("debrief de %s", r.get("scenario_title"))
                .isNotBlank()
                .contains("O que dá para aprender"));
    }

    @Test
    void todoAtivoDasRodadasTemFraseDeRevelacao() {
        Integer semFrase = jdbc.queryForObject("""
                SELECT COUNT(*) FROM assets a
                JOIN competition_assets ca ON ca.asset_id = a.id
                WHERE a.reveal_note IS NULL OR a.reveal_note = ''
                """, Integer.class);
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM competition_assets", Integer.class);

        assertThat(total).isEqualTo(40);
        assertThat(semFrase).isZero();
    }

    @Test
    void rodadaDe2011NaoContaOFuturoAoJogador() {
        String descricao = jdbc.queryForObject(
                "SELECT scenario_description FROM competitions WHERE start_year = 2011", String.class);

        // Nada depois de janeiro de 2011: nem a Selic mínima de 2012, nem a alta de 2013.
        assertThat(descricao).startsWith("Começo de 2011").doesNotContain("7,25%").doesNotContain("2013");
    }

    @Test
    void rodadaDe2004NaoDizMaisQueOPaisJaTinhaGrauDeInvestimento() {
        String descricao = jdbc.queryForObject(
                "SELECT scenario_description FROM competitions WHERE start_year = 2004", String.class);

        assertThat(descricao)
                .doesNotContain("acabara de obter o grau de investimento")
                .contains("buscava o grau de investimento");
    }
}
