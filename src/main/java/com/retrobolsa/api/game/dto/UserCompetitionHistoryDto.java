package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class UserCompetitionHistoryDto {
    int roundNumber;
    String scenarioTitle;
    BigDecimal totalReturn;
    BigDecimal finalValue;
    Integer rank;
    LocalDateTime submittedAt;
}
