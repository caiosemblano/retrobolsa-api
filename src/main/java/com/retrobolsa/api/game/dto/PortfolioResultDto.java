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
    }
}
