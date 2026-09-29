package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Uma rodada já revelada, disponível para treino, com os treinos do jogador nela. */
@Value
@Builder
public class PracticeRoundDto {
    String id;
    int round;
    String scenarioTitle;
    int startYear;
    int endYear;
    int assetCount;
    /** Quantas vezes o jogador já treinou nesta rodada. */
    long runs;
    /** A melhor rentabilidade dos treinos dele aqui, em %; nula se ainda não treinou. */
    BigDecimal bestReturn;
}
