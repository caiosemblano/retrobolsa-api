package com.retrobolsa.api.game.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingResponseDto {
    private UUID userId;
    private String username;
    private int rank;
    private BigDecimal totalReturn;
    private BigDecimal finalValue;
    private int roundNumber;
    private LocalDateTime submittedAt;
    /** Status da rodada: "open", "simulated", "revealed", etc. */
    private String roundStatus;
}
