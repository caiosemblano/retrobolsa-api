package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AchievementResponseDto {
    String code;
    String title;
    String description;
    String rarity;
    boolean unlocked;
    /** Null enquanto a conquista estiver bloqueada. */
    LocalDateTime unlockedAt;
}
