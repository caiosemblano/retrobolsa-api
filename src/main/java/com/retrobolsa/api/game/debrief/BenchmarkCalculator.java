package com.retrobolsa.api.game.debrief;

import com.retrobolsa.api.game.dto.BenchmarkDto;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
import com.retrobolsa.api.game.macro.MacroIndicator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quanto o orçamento teria rendido nas referências do mercado, nos mesmos anos
 * que o SimulationEngine simula: do start_year ao end_year - 1, com o gráfico
 * começando no start_year com o orçamento inteiro.
 */
@Component
public class BenchmarkCalculator {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private record Referencia(String code, String name, Function<MacroIndicator, BigDecimal> taxaNoAno) {}

    private static final List<Referencia> REFERENCIAS = List.of(
            new Referencia("CDI", "CDI", MacroIndicator::getCdi),
            new Referencia("POUPANCA", "Poupança", MacroIndicator::getPoupanca),
            new Referencia("IBOVESPA", "Ibovespa", MacroIndicator::getIbovespa),
            new Referencia("IPCA", "Inflação (IPCA)", MacroIndicator::getIpca));

    /** Lista vazia se falta algum ano do período: melhor não comparar do que comparar pela metade. */
    public List<BenchmarkDto> calculate(List<MacroIndicator> indicators, BigDecimal budget, int startYear, int endYear) {
        Map<Integer, MacroIndicator> porAno = indicators.stream()
                .collect(Collectors.toMap(MacroIndicator::getYear, Function.identity()));
        for (int ano = startYear; ano < endYear; ano++) {
            if (!porAno.containsKey(ano)) return List.of();
        }

        List<BenchmarkDto> resultado = new ArrayList<>();
        for (Referencia referencia : REFERENCIAS) {
            List<PortfolioResultDto.ChartPoint> grafico = new ArrayList<>();
            BigDecimal valor = budget;
            grafico.add(ponto(startYear, valor));
            for (int ano = startYear; ano < endYear; ano++) {
                BigDecimal taxa = referencia.taxaNoAno().apply(porAno.get(ano));
                valor = valor.multiply(BigDecimal.ONE.add(taxa.divide(CEM, MC)), MC);
                grafico.add(ponto(ano + 1, valor));
            }
            BigDecimal rentabilidade = valor.subtract(budget).divide(budget, MC)
                    .multiply(CEM).setScale(2, RoundingMode.HALF_UP);
            resultado.add(BenchmarkDto.builder()
                    .code(referencia.code())
                    .name(referencia.name())
                    .totalReturn(rentabilidade)
                    .chartData(grafico)
                    .build());
        }
        return resultado;
    }

    private PortfolioResultDto.ChartPoint ponto(int ano, BigDecimal valor) {
        return PortfolioResultDto.ChartPoint.builder().year(ano).value(valor.setScale(2, RoundingMode.HALF_UP)).build();
    }
}
