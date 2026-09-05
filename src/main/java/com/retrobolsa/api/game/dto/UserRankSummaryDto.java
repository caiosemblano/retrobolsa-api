package com.retrobolsa.api.game.dto;

import lombok.*;

import java.util.UUID;

/**
 * DTO retornado pelo endpoint GET /api/rankings/me.
 * Contém a posição do usuário autenticado no ranking global e na rodada ativa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRankSummaryDto {

    private UUID userId;
    private String username;

    /** Posição no ranking global (por totalScore). */
    private int globalRank;

    /** Total de jogadores no ranking global. */
    private long totalGlobalPlayers;

    /** Pontuação acumulada do usuário. */
    private int totalScore;

    /** Número de rodadas disputadas pelo usuário. */
    private int competitionsPlayed;

    /**
     * Posição do usuário na rodada mais recente (ativa ou simulada).
     * Null se o usuário não participou da rodada.
     */
    private Integer activeRoundRank;

    /**
     * Número da rodada ativa ou mais recente.
     * Null se não há rodada disponível.
     */
    private Integer activeRoundNumber;

    /**
     * Status da rodada ativa/mais recente.
     * Ex: "open", "simulated", "revealed".
     */
    private String activeRoundStatus;
}
