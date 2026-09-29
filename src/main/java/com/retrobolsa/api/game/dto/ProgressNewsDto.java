package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** O que o jogador ganhou e ainda não viu comemorado no app. */
@Value
@Builder
public class ProgressNewsDto {
    List<Item> items;
    int xpGained;
    /** Subiu de nível com estes ganhos. */
    boolean levelUp;
    int level;
    String levelTitle;
    /** As conquistas desbloqueadas entre os ganhos, para mostrar o emblema. */
    List<AchievementResponseDto> achievements;

    @Value
    @Builder
    public static class Item {
        UUID id;
        String source;
        /** Descrição legível, ex.: "Aula concluída: O que é ROE?". */
        String label;
        int amount;
    }
}
