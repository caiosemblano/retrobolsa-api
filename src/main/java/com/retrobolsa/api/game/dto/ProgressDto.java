package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

/** XP, nível e sequência de semanas do jogador. */
@Value
@Builder
public class ProgressDto {
    int xp;
    int level;
    String levelTitle;
    /** XP em que o nível atual começa (para a barra de progresso). */
    int levelMinXp;
    /** Próximo nível; nulos no nível máximo. */
    Integer nextLevel;
    String nextLevelTitle;
    Integer nextLevelMinXp;
    /** Semanas seguidas com alguma atividade, até esta. */
    int streakWeeks;
}
