package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.CompetitionResponseDto;
import com.retrobolsa.api.game.competition.CompetitionService;
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

    /** Retorna a rodada com status "open". Usado durante o período de apostas. */
    @GetMapping("/active")
    public ResponseEntity<CompetitionResponseDto> getActiveCompetition() {
        return ResponseEntity.ok(competitionService.getActiveCompetition());
    }

    /**
     * Retorna a rodada mais recente independente do status (open, closed, simulated, revealed).
     * Usado para mostrar o estado atual da competição após o fechamento.
     */
    @GetMapping("/latest")
    public ResponseEntity<CompetitionResponseDto> getLatestCompetition() {
        return ResponseEntity.ok(competitionService.getLatestCompetition());
    }
}
