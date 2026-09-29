package com.retrobolsa.api.game.progress;

/** De onde veio o XP. O ref_id de cada evento aponta para a coisa que o gerou. */
public enum XpSource {
    /** Aula concluída (ref: id da aula). */
    LESSON,
    /** Nota máxima no quiz de uma aula (ref: id da aula). */
    QUIZ_PERFECT,
    /** Carteira enviada numa rodada (ref: id da rodada). */
    PORTFOLIO,
    /** Carteira que terminou a rodada acima do CDI (ref: id da rodada). */
    BEAT_CDI,
    /** Conquista desbloqueada (ref: código da conquista). */
    ACHIEVEMENT,
    /** Treino numa rodada já revelada (ref: id da rodada). */
    PRACTICE
}
