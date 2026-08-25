package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions")
public class CompetitionAdminController {

    private final CompetitionRepository competitionRepository;
    private final PortfolioService portfolioService;

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> close(@PathVariable UUID id) {
        Competition competition = find(id);
        if (!"open".equals(competition.getStatus())) {
            throw new IllegalArgumentException("A rodada precisa estar aberta para ser encerrada");
        }
        competition.setStatus("closed");
        competitionRepository.save(competition);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/simulate")
    public ResponseEntity<Void> simulate(@PathVariable UUID id) {
        portfolioService.simulateCompetition(find(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reveal")
    public ResponseEntity<Void> reveal(@PathVariable UUID id) {
        Competition competition = find(id);
        if (!"simulated".equals(competition.getStatus())) {
            throw new IllegalArgumentException("A rodada precisa ser simulada antes da revelacao");
        }
        competition.setStatus("revealed");
        competitionRepository.save(competition);
        return ResponseEntity.noContent().build();
    }

    private Competition find(UUID id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rodada nao encontrada"));
    }
}
