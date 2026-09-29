package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.CompetitionResponseDto;
import com.retrobolsa.api.game.dto.PortfolioResultDto;
import com.retrobolsa.api.game.dto.PracticeRequestDto;
import com.retrobolsa.api.game.dto.PracticeRoundDto;
import com.retrobolsa.api.game.practice.PracticeService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;
    private final UserRepository userRepository;

    /** Rodadas já reveladas, com quantas vezes o jogador treinou em cada uma. */
    @GetMapping
    public ResponseEntity<List<PracticeRoundDto>> rounds(Authentication authentication) {
        return ResponseEntity.ok(practiceService.rounds(resolveUser(authentication).getId()));
    }

    /** Os dados para montar a carteira de treino de uma rodada revelada. */
    @GetMapping("/{competitionId}")
    public ResponseEntity<CompetitionResponseDto> round(@PathVariable UUID competitionId) {
        return ResponseEntity.ok(practiceService.round(competitionId));
    }

    /** Simula a carteira de treino na hora e devolve o resultado completo. */
    @PostMapping("/{competitionId}")
    public ResponseEntity<PortfolioResultDto> practice(@PathVariable UUID competitionId,
                                                       @Valid @RequestBody PracticeRequestDto request,
                                                       Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(practiceService.practice(user.getId(), competitionId, request.getAllocations()));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
