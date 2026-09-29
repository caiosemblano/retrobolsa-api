package com.retrobolsa.api.game.progress;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LevelsAndStreaksTest {

    @Test
    void nivelMudaExatamenteNaFronteira() {
        assertThat(Levels.of(0).title()).isEqualTo("Curioso");
        assertThat(Levels.of(49).number()).isEqualTo(1);
        assertThat(Levels.of(50).title()).isEqualTo("Aprendiz");
        assertThat(Levels.of(499).number()).isEqualTo(4);
        assertThat(Levels.of(500).title()).isEqualTo("Analista");
        assertThat(Levels.of(1300).title()).isEqualTo("Lenda do Pregão");
        assertThat(Levels.of(99_999).number()).isEqualTo(8);
    }

    @Test
    void proximoNivelAteOTopo() {
        assertThat(Levels.next(Levels.of(0))).get().extracting(Levels.Level::minXp).isEqualTo(50);
        assertThat(Levels.next(Levels.of(1300))).isEmpty();
    }

    @Test
    void niveisTemCurvaCrescente() {
        for (int i = 1; i < Levels.ALL.size(); i++) {
            int degrau = Levels.ALL.get(i).minXp() - Levels.ALL.get(i - 1).minXp();
            int anterior = i == 1 ? 0 : Levels.ALL.get(i - 1).minXp() - Levels.ALL.get(i - 2).minXp();
            assertThat(degrau).as("degrau do nível %d", i + 1).isGreaterThanOrEqualTo(anterior);
        }
    }

    // 2026-09-29 é uma terça-feira.
    private static final LocalDate HOJE = LocalDate.of(2026, 9, 29);

    @Test
    void semanasSeguidasAteEstaSemana() {
        List<LocalDate> dias = List.of(HOJE, HOJE.minusWeeks(1), HOJE.minusWeeks(2).minusDays(1));
        assertThat(Streaks.currentWeeks(dias, HOJE)).isEqualTo(3);
    }

    @Test
    void semanaAtualSemAtividadeAindaNaoQuebraASequencia() {
        List<LocalDate> dias = List.of(HOJE.minusWeeks(1), HOJE.minusWeeks(2));
        assertThat(Streaks.currentWeeks(dias, HOJE)).isEqualTo(2);
    }

    @Test
    void umaSemanaVaziaNoMeioQuebraASequencia() {
        List<LocalDate> dias = List.of(HOJE, HOJE.minusWeeks(2), HOJE.minusWeeks(3));
        assertThat(Streaks.currentWeeks(dias, HOJE)).isEqualTo(1);
    }

    @Test
    void variasAtividadesNaMesmaSemanaContamUmaVez() {
        // Segunda e domingo da mesma semana.
        List<LocalDate> dias = List.of(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));
        assertThat(Streaks.currentWeeks(dias, LocalDate.of(2026, 10, 4))).isEqualTo(1);
    }

    @Test
    void semAtividadeRecenteASequenciaEZero() {
        assertThat(Streaks.currentWeeks(List.of(HOJE.minusWeeks(3)), HOJE)).isZero();
        assertThat(Streaks.currentWeeks(List.of(), HOJE)).isZero();
    }
}
