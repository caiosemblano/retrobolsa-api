package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/** A carteira que o jogador enviou na rodada aberta, para ele poder revê-la e editá-la. */
@Value
@Builder
public class CurrentPortfolioDto {
    String competitionId;
    List<Position> allocations;

    @Value
    @Builder
    public static class Position {
        String assetId;
        BigDecimal amount;
    }
}
