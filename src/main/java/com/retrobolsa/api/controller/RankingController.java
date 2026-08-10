package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.GlobalRankingResponseDto;
import com.retrobolsa.api.game.dto.RankingResponseDto;
import com.retrobolsa.api.game.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<?> getRankings(
            @RequestParam(required = false) UUID competitionId,
            @RequestParam(required = false) Integer roundNumber,
            @RequestParam(required = false) String type) {

        if ("global".equalsIgnoreCase(type) || "general".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(rankingService.getGlobalRanking());
        }
        
        if ("quinzenal".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(rankingService.getQuinzenalRanking());
        }

        if ("season".equalsIgnoreCase(type)) {
            // Placeholder: retornar ranking da temporada (pode ser o mesmo que o global por enquanto ou vazio)
            return ResponseEntity.ok(rankingService.getGlobalRanking());
        }

        List<RankingResponseDto> ranking = rankingService.getRanking(competitionId, roundNumber);
        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/global")
    public ResponseEntity<List<GlobalRankingResponseDto>> getGlobalRanking() {
        return ResponseEntity.ok(rankingService.getGlobalRanking());
    }
}
