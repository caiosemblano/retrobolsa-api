package com.retrobolsa.api.game.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDto {
    private String id;
    private String type;
    private String anonymousName;
    private String sector;
    private String bondType;
    private BigDecimal rate;
    private IndicatorsDto indicators;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndicatorsDto {
        private BigDecimal pl;
        /** Retorno sobre o patrimônio, em %. */
        private BigDecimal roe;
        /** Dividendos pagos no ano sobre o preço da ação, em %. */
        private BigDecimal dividendYield;
        private BigDecimal lvp;
        private Boolean lucroPositivo;
        private BigDecimal cagrLucro;
        private BigDecimal cagrReceita;
        private BigDecimal margemEbitda;
    }
}
