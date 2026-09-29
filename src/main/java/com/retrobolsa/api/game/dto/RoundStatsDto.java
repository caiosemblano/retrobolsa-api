package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Como a rodada foi para todo mundo, para o jogador se situar além da própria posição. */
@Value
@Builder
public class RoundStatsDto {
    int participants;
    /** Rentabilidade mediana das carteiras da rodada, em %; nula se ninguém tem resultado. */
    BigDecimal medianReturn;
    /** O ativo da rodada que mais rendeu no período; nulo se não há dados. */
    BestAssetDto bestAsset;

    @Value
    @Builder
    public static class BestAssetDto {
        String anonymousName;
        /** Só depois da revelação. */
        String realName;
        BigDecimal returnPct;
    }
}
