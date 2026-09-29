package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Um indicador do cenário econômico da rodada, como estava no ano anterior ao início. */
@Value
@Builder
public class EconomicIndicatorDto {
    /** Identificador estável (SELIC, IPCA, DOLAR, PIB); o app usa para o ícone e o glossário. */
    String code;
    String label;
    BigDecimal value;
    /** Unidade do valor: "% a.a.", "% no ano" ou "R$". */
    String unit;
    int year;
}
