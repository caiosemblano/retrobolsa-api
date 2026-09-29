package com.retrobolsa.api.game.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResultDto {
    private int rank;
    private BigDecimal rentability;
    private BigDecimal annualReturn;
    private BigDecimal portfolioValue;
    private List<ChartPoint> chartData;
    private List<RevealedAssetDto> revealedAssets;
    private String period;
    /** O mesmo orçamento no CDI, na poupança, no Ibovespa e corrigido pela inflação. */
    private List<BenchmarkDto> benchmarks;
    private RoundStatsDto roundStats;
    /** O que aconteceu de verdade no período; só depois da revelação. */
    private String debrief;
    private List<TipDto> tips;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartPoint {
        private int year;
        private BigDecimal value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevealedAssetDto {
        private String id;
        private String anonymousName;
        private String realName;
        private String ticker;
        private String type;
        private String sector;
        private String bondType;
        private BigDecimal amountInvested;
        private BigDecimal finalValue;
        /** Quanto o ativo rendeu no período, em %. */
        private BigDecimal returnPct;
        /** Quanto ele somou (ou tirou) da rentabilidade da carteira, em pontos percentuais do orçamento. */
        private BigDecimal contribution;
        /** Frase sobre o ativo no período; só depois da revelação. */
        private String revealNote;
    }
}
