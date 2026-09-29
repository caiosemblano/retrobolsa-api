package com.retrobolsa.api.game.mission;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Regras puras das missões: a semana e o sorteio. Sem banco, testadas diretamente. */
public final class Missions {

    /** Quantas missões valem por semana. */
    public static final int PER_WEEK = 3;

    /** Os códigos da V24, em ordem alfabética (a ordem entra no sorteio). */
    public static final List<String> CODES = List.of(
            "AULAS_2", "CARTEIRA_3", "DOIS_DIAS", "QUIZ_PERFEITO", "TREINO", "TREINO_2");

    private Missions() {}

    /** "2026-W40": a semana ISO, de segunda a domingo. */
    public static String isoWeek(LocalDate date) {
        return String.format("%d-W%02d",
                date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    /** O último instante da semana ISO da data: domingo, 23:59:59. */
    public static LocalDateTime weekEnd(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.of(23, 59, 59));
    }

    /**
     * As missões da semana: um embaralhamento com a semana como semente. O algoritmo
     * do {@link Random} é fixo pela especificação do Java, então o sorteio é o mesmo
     * para todos, em qualquer servidor, a semana inteira.
     *
     * @param codes os códigos dos modelos, em ordem alfabética
     */
    public static List<String> draw(List<String> codes, String isoWeek) {
        List<String> shuffled = new ArrayList<>(codes);
        Collections.shuffle(shuffled, new Random(seed(isoWeek)));
        return List.copyOf(shuffled.subList(0, Math.min(PER_WEEK, shuffled.size())));
    }

    /** "2026-W40" → 202640. */
    static long seed(String isoWeek) {
        String[] parts = isoWeek.split("-W");
        return Long.parseLong(parts[0]) * 100 + Long.parseLong(parts[1]);
    }
}
