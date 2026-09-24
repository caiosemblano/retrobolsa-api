package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.competition.CompetitionAdminService;
import com.retrobolsa.api.game.dto.AdminAssetDto;
import com.retrobolsa.api.game.dto.CreateCompetitionRequestDto;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import com.retrobolsa.api.game.competition.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions")
public class CompetitionAdminController {

    private final CompetitionRepository competitionRepository;
    private final CompetitionAdminService competitionAdminService;
    private final PortfolioService portfolioService;
    private final CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<List<Competition>> list() {
        return ResponseEntity.ok(competitionService.listCompetitions());
    }

    @PostMapping
    public ResponseEntity<Competition> create(@Valid @RequestBody CreateCompetitionRequestDto request) {
        return ResponseEntity.status(201).body(competitionAdminService.create(request));
    }

    /** Ativos disponíveis para montar uma rodada, com os anos que têm dados. */
    @GetMapping("/assets")
    public ResponseEntity<List<AdminAssetDto>> listAssets() {
        return ResponseEntity.ok(competitionAdminService.listAssets());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id) {
        Competition competition = find(id);
        if (!"draft".equals(competition.getStatus())) {
            throw new IllegalArgumentException("A rodada precisa estar em rascunho para ser publicada");
        }
        if (competitionRepository.findByStatus("open").isPresent()) {
            throw new IllegalArgumentException("Ja existe uma rodada aberta");
        }
        competition.setStatus("open");
        competitionRepository.save(competition);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/next-round")
    public ResponseEntity<Void> nextRound() {
        competitionService.nextRound();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetGame() {
        competitionService.resetGame();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> start(@PathVariable UUID id) {
        competitionService.startRound(id);
        return ResponseEntity.noContent().build();
    }

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

    @PostMapping("/{id}/quick-simulate")
    public ResponseEntity<Void> quickSimulate(@PathVariable UUID id) {
        Competition competition = find(id);
        competition.setStatus("closed");
        competitionRepository.save(competition);
        portfolioService.simulateCompetition(competition);
        competition.setStatus("revealed");
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
