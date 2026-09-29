package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/** As 3 missões da semana e o progresso do jogador em cada uma. */
@Value
@Builder
public class MissionWeekDto {
    String week;
    /** Domingo, 23:59:59: quando as missões trocam. */
    LocalDateTime endsAt;
    List<Mission> missions;

    @Value
    @Builder
    public static class Mission {
        String code;
        String title;
        String description;
        int target;
        /** Quanto já andou, no máximo o alvo. */
        int progress;
        boolean completed;
        int xp;
    }
}
