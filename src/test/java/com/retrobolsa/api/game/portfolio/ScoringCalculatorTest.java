package com.retrobolsa.api.game.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScoringCalculator — Testes Unitários")
class ScoringCalculatorTest {

    @Test
    @DisplayName("converte rentabilidade percentual em pontos arredondando HALF_UP")
    void deveArredondarHalfUp() {
        assertThat(ScoringCalculator.pointsFromReturn(new BigDecimal("15.23"))).isEqualTo(15);
        assertThat(ScoringCalculator.pointsFromReturn(new BigDecimal("14.50"))).isEqualTo(15);
        assertThat(ScoringCalculator.pointsFromReturn(new BigDecimal("14.49"))).isEqualTo(14);
    }

    @Test
    @DisplayName("rentabilidade negativa gera pontos negativos (piso é aplicado no score acumulado)")
    void deveManterSinalNegativo() {
        assertThat(ScoringCalculator.pointsFromReturn(new BigDecimal("-8.60"))).isEqualTo(-9);
    }

    @Test
    @DisplayName("rentabilidade nula vale zero ponto")
    void deveTratarNulo() {
        assertThat(ScoringCalculator.pointsFromReturn(null)).isZero();
        assertThat(ScoringCalculator.pointsFromReturn(BigDecimal.ZERO)).isZero();
    }
}
