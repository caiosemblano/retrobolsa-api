package com.retrobolsa.api.game.macro;

import com.retrobolsa.api.controller.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A V14 traz os indicadores oficiais de 1995 a 2025. Confere cobertura e alguns
 * valores conhecidos, para pegar um gerador que troque série, escala ou ano.
 */
class MacroIndicatorMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MacroIndicatorRepository repository;

    @Test
    void cobreTodosOsAnosDe1995a2025SemBuracos() {
        List<MacroIndicator> anos = repository.findAllByYearBetweenOrderByYearAsc(1995, 2025);

        assertThat(anos).hasSize(31);
        assertThat(anos).extracting(MacroIndicator::getYear)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1995, 2025).boxed().toList());
        assertThat(anos).allSatisfy(ano -> {
            assertThat(ano.getSelic()).isNotNull();
            assertThat(ano.getCdi()).isNotNull();
            assertThat(ano.getIpca()).isNotNull();
            assertThat(ano.getPoupanca()).isNotNull();
            assertThat(ano.getIbovespa()).isNotNull();
            assertThat(ano.getDolar()).isNotNull();
            assertThat(ano.getPib()).isNotNull();
        });
    }

    @Test
    void metaDaSelicSoExisteDepoisQueOCopomPassouADefinirUma() {
        assertThat(repository.findById(1998)).get().extracting(MacroIndicator::getSelicMeta).isNull();
        assertThat(repository.findById(1999)).get().extracting(MacroIndicator::getSelicMeta).isNotNull();
    }

    @Test
    void valoresConhecidosBatemComAsFontesOficiais() {
        // IPCA oficial do IBGE e meta da Selic no fim do ano, amplamente divulgados.
        MacroIndicator ano2015 = repository.findById(2015).orElseThrow();
        assertThat(ano2015.getIpca()).isEqualByComparingTo("10.67");
        assertThat(ano2015.getSelicMeta()).isEqualByComparingTo("14.25");

        MacroIndicator ano2003 = repository.findById(2003).orElseThrow();
        assertThat(ano2003.getSelicMeta()).isEqualByComparingTo("16.50");

        MacroIndicator ano2020 = repository.findById(2020).orElseThrow();
        assertThat(ano2020.getSelicMeta()).isEqualByComparingTo("2.00");
        assertThat(ano2020.getPib()).isNegative();

        // 2008: crise mundial, a bolsa caiu mais de 40%.
        assertThat(repository.findById(2008).orElseThrow().getIbovespa()).isLessThan(new java.math.BigDecimal("-40"));
    }

    @Test
    void cdiFicaPertoDaSelicEPoupancaRendeMenosQueOCdi() {
        repository.findAllByYearBetweenOrderByYearAsc(2000, 2025).forEach(ano -> {
            assertThat(ano.getCdi().subtract(ano.getSelic()).abs())
                    .as("CDI x Selic em %d", ano.getYear()).isLessThan(new java.math.BigDecimal("1"));
            assertThat(ano.getPoupanca())
                    .as("poupança x CDI em %d", ano.getYear()).isLessThan(ano.getCdi());
        });
    }
}
