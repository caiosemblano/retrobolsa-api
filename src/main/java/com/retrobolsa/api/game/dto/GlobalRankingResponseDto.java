package com.retrobolsa.api.game.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalRankingResponseDto {
    private String username;
    private int rank;
    private int totalScore;
}
