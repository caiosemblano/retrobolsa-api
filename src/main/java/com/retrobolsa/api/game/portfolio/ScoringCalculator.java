package com.retrobolsa.api.game.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converte a rentabilidade (percentual) de uma rodada em pontos de ranking. */
public final class ScoringCalculator {

    private ScoringCalculator() {
    }

    public static int pointsFromReturn(BigDecimal totalReturn) {
        return totalReturn == null ? 0
                : totalReturn.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
