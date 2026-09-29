package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import com.retrobolsa.api.game.debrief.DebriefAdvisor.Input;
import com.retrobolsa.api.game.debrief.DebriefAdvisor.Position;
import com.retrobolsa.api.game.dto.TipDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Toda aula para a qual uma dica aponta existe no seed e pertence ao módulo indicado. */
class DebriefLinksIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DebriefAdvisor advisor;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void linksDasDicasApontamParaAulasQueExistem() {
        // Uma carteira que dispara todas as regras com link: perdeu para inflação e CDI,
        // concentrada, com dinheiro parado e só ações com Selic alta.
        List<TipDto> dicas = new java.util.ArrayList<>(advisor.advise(new Input(new BigDecimal("100000"),
                List.of(new Position("Empresa A", "stock", new BigDecimal("80000"), new BigDecimal("60000"))),
                new BigDecimal("-20"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("16"))));
        dicas.addAll(advisor.advise(new Input(new BigDecimal("100000"),
                List.of(new Position("Empresa A", "stock", new BigDecimal("50000"), new BigDecimal("60000")),
                        new Position("Empresa B", "stock", new BigDecimal("50000"), new BigDecimal("60000"))),
                new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("5"), new BigDecimal("16"))));

        List<TipDto> comLink = dicas.stream().filter(d -> d.getArticleId() != null).toList();
        assertThat(comLink).extracting(TipDto::getCode)
                .contains("PERDEU_PARA_INFLACAO", "PERDEU_PARA_CDI", "CONCENTRADA", "DINHEIRO_PARADO", "SO_ACOES_COM_SELIC_ALTA");
        comLink.forEach(dica -> assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM articles WHERE id = ?::uuid AND module_id = ?::uuid",
                Integer.class, dica.getArticleId(), dica.getModuleId()))
                .as("aula da dica %s", dica.getCode()).isEqualTo(1));
    }
}
