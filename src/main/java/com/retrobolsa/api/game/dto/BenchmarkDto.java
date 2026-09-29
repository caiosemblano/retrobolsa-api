package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/** Quanto o orçamento da rodada teria virado numa referência (CDI, poupança, Ibovespa, inflação). */
@Value
@Builder
public class BenchmarkDto {
    /** CDI, POUPANCA, IBOVESPA ou IPCA. */
    String code;
    String name;
    /** Rentabilidade no período, em %. */
    BigDecimal totalReturn;
    /** Mesmos anos do gráfico da carteira, para desenhar as linhas juntas. */
    List<PortfolioResultDto.ChartPoint> chartData;
}
