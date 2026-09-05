package com.retrobolsa.api.game.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalRankingResponseDto {
    private UUID userId;
    private String username;
    private int rank;
    private int totalScore;
    /** Número de rodadas em que o usuário participou. */
    private int competitionsPlayed;
}
