package com.retrobolsa.api.game.mission;

/** O que faz uma missão andar. A referência de cada evento diz o que conta como "diferente". */
public enum MissionEvent {
    /** Aula concluída (ref: id da aula). */
    LESSON,
    /** Nota máxima num quiz (ref: id da aula). */
    QUIZ_PERFECT,
    /** Carteira com 3 ativos ou mais, de verdade ou de treino (ref: "rodada:<id>" ou "treino:<id>"). */
    PORTFOLIO_3,
    /** Treino numa rodada passada (ref: id da rodada). */
    PRACTICE,
    /** Abriu o app (ref: o dia). */
    VISIT
}
