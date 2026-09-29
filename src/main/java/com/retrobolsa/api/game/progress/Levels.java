package com.retrobolsa.api.game.progress;

import java.util.List;
import java.util.Optional;

/**
 * Os 8 níveis. A curva cresce devagar no começo (dá para subir já na primeira
 * aula e na primeira rodada) e fica exigente no fim: somando tudo o que o jogo
 * oferece hoje (aulas, quizzes perfeitos, rodadas e conquistas) dá cerca de
 * 1.500 XP, então "Lenda do Pregão" pede fazer quase tudo.
 */
public final class Levels {

    private Levels() {
    }

    public record Level(int number, String title, int minXp) {}

    public static final List<Level> ALL = List.of(
            new Level(1, "Curioso", 0),
            new Level(2, "Aprendiz", 50),
            new Level(3, "Estagiário", 150),
            new Level(4, "Analista Jr.", 300),
            new Level(5, "Analista", 500),
            new Level(6, "Gestor", 750),
            new Level(7, "Estrategista", 1000),
            new Level(8, "Lenda do Pregão", 1300));

    public static Level of(int xp) {
        Level current = ALL.get(0);
        for (Level level : ALL) {
            if (xp >= level.minXp()) current = level;
        }
        return current;
    }

    public static Optional<Level> next(Level level) {
        return level.number() < ALL.size() ? Optional.of(ALL.get(level.number())) : Optional.empty();
    }
}
