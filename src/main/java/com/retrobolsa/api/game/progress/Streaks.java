package com.retrobolsa.api.game.progress;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sequência de semanas seguidas com alguma atividade (algum ganho de XP).
 * Semanal, e não diária, de propósito: combina com o ritmo das rodadas e das
 * aulas e não cobra o aluno todo dia.
 */
public final class Streaks {

    private Streaks() {
    }

    /**
     * Semanas seguidas com atividade, terminando nesta semana. A semana atual ainda
     * em andamento não quebra a sequência: se não houve atividade nela ainda, conta
     * a partir da semana passada.
     */
    public static int currentWeeks(Collection<LocalDate> activityDays, LocalDate today) {
        Set<LocalDate> weeks = activityDays.stream().map(Streaks::weekStart).collect(Collectors.toSet());
        LocalDate week = weekStart(today);
        if (!weeks.contains(week)) week = week.minusWeeks(1);
        int count = 0;
        while (weeks.contains(week)) {
            count++;
            week = week.minusWeeks(1);
        }
        return count;
    }

    static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
