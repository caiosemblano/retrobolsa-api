package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.competition.CompetitionService;
import com.retrobolsa.api.game.dto.CompetitionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping("/active")
    public ResponseEntity<CompetitionResponseDto> getActiveCompetition() {
        return ResponseEntity.ok(competitionService.getActiveCompetition());
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/next-round")
    public ResponseEntity<String> nextRound() {
        competitionService.nextRound();
        return ResponseEntity.ok("Avancado para a proxima rodada");
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/reset")
    public ResponseEntity<String> resetGame() {
        competitionService.resetGame();
        return ResponseEntity.ok("Jogo resetado para a rodada 1");
    }
}
