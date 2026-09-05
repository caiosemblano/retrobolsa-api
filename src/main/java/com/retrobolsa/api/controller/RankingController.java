package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.GlobalRankingResponseDto;
import com.retrobolsa.api.game.dto.RankingResponseDto;
import com.retrobolsa.api.game.dto.UserRankSummaryDto;
import com.retrobolsa.api.game.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para rankings.
 *
 * <ul>
 *   <li>{@code GET /api/rankings?type=global}     — ranking global paginado</li>
 *   <li>{@code GET /api/rankings?type=quinzenal}  — ranking da rodada ativa/mais recente</li>
 *   <li>{@code GET /api/rankings?type=season}     — alias para global (a implementar por temporada)</li>
 *   <li>{@code GET /api/rankings?competitionId=X} — ranking de rodada específica por ID</li>
 *   <li>{@code GET /api/rankings?roundNumber=N}   — ranking de rodada específica por número</li>
 *   <li>{@code GET /api/rankings/global}          — ranking global dedicado com paginação</li>
 *   <li>{@code GET /api/rankings/me}              — posição do usuário autenticado</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    // -------------------------------------------------------------------------
    // GET /api/rankings (rota principal com type e filtros de rodada)
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<?> getRankings(
            @RequestParam(required = false) UUID competitionId,
            @RequestParam(required = false) Integer roundNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page) {

        if ("global".equalsIgnoreCase(type) || "general".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(rankingService.getGlobalRanking(limit, page));
        }

        if ("quinzenal".equalsIgnoreCase(type) || "active".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(rankingService.getQuinzenalRanking());
        }

        if ("season".equalsIgnoreCase(type)) {
            // Por ora season é equivalente ao global; futuramente filtrar por temporada
            return ResponseEntity.ok(rankingService.getGlobalRanking(limit, page));
        }

        // Sem type → ranking de rodada específica (requer competitionId ou roundNumber)
        List<RankingResponseDto> ranking = rankingService.getRanking(competitionId, roundNumber);
        return ResponseEntity.ok(ranking);
    }

    // -------------------------------------------------------------------------
    // GET /api/rankings/global (rota dedicada com paginação)
    // -------------------------------------------------------------------------

    @GetMapping("/global")
    public ResponseEntity<List<GlobalRankingResponseDto>> getGlobalRanking(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page) {
        return ResponseEntity.ok(rankingService.getGlobalRanking(limit, page));
    }

    // -------------------------------------------------------------------------
    // GET /api/rankings/me (posição do usuário autenticado)
    // -------------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<UserRankSummaryDto> getMyRank(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserRankSummaryDto summary = rankingService.getUserRankSummary(userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }
}
