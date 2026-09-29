package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.game.dto.BenchmarkDto;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
import com.retrobolsa.api.game.macro.MacroIndicator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkCalculatorTest {

    private static final BigDecimal ORCAMENTO = new BigDecimal("100000.00");

    private final BenchmarkCalculator calculator = new BenchmarkCalculator();

    private MacroIndicator ano(int ano, String cdi, String poupanca, String ibovespa, String ipca) {
        return MacroIndicator.builder()
                .year(ano)
                .selic(new BigDecimal(cdi)).cdi(new BigDecimal(cdi))
                .poupanca(new BigDecimal(poupanca)).ibovespa(new BigDecimal(ibovespa)).ipca(new BigDecimal(ipca))
                .dolar(BigDecimal.ONE).pib(BigDecimal.ONE)
                .build();
    }

    private BenchmarkDto porCodigo(List<BenchmarkDto> lista, String code) {
        return lista.stream().filter(b -> b.getCode().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void compoeAnoAAnoOsMesmosAnosQueOMotorSimula() {
        // Rodada 2020–2022: o motor aplica os retornos de 2020 e 2021.
        List<BenchmarkDto> lista = calculator.calculate(List.of(
                ano(2020, "10", "5", "-20", "4"),
                ano(2021, "10", "5", "50", "6"),
                ano(2022, "99", "99", "99", "99")), ORCAMENTO, 2020, 2022);

        BenchmarkDto cdi = porCodigo(lista, "CDI");
        assertThat(cdi.getTotalReturn()).isEqualByComparingTo("21.00");
        assertThat(cdi.getChartData()).extracting(PortfolioResultDto.ChartPoint::getYear).containsExactly(2020, 2021, 2022);
        assertThat(cdi.getChartData()).extracting(p -> p.getValue().toPlainString())
                .containsExactly("100000.00", "110000.00", "121000.00");

        // Perdeu 20% e depois ganhou 50%: 0,8 x 1,5 = 1,2.
        assertThat(porCodigo(lista, "IBOVESPA").getTotalReturn()).isEqualByComparingTo("20.00");
        assertThat(porCodigo(lista, "POUPANCA").getTotalReturn()).isEqualByComparingTo("10.25");
        assertThat(porCodigo(lista, "IPCA").getTotalReturn()).isEqualByComparingTo("10.24");
    }

    @Test
    void entregaAsQuatroReferenciasNaOrdemDoGrafico() {
        List<BenchmarkDto> lista = calculator.calculate(List.of(ano(2020, "1", "1", "1", "1")), ORCAMENTO, 2020, 2021);

        assertThat(lista).extracting(BenchmarkDto::getCode).containsExactly("CDI", "POUPANCA", "IBOVESPA", "IPCA");
        assertThat(lista).extracting(BenchmarkDto::getName).containsExactly("CDI", "Poupança", "Ibovespa", "Inflação (IPCA)");
    }

    @Test
    void semTodosOsAnosDoPeriodoNaoCompara() {
        List<BenchmarkDto> lista = calculator.calculate(
                List.of(ano(2020, "10", "5", "1", "4")), ORCAMENTO, 2020, 2022);

        assertThat(lista).isEmpty();
    }
}
