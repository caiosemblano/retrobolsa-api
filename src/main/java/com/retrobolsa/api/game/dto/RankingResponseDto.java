package com.retrobolsa.api.game.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingResponseDto {
    private String username;
    private int rank;
    private BigDecimal totalReturn;
    private BigDecimal finalValue;
    private int roundNumber;
}
