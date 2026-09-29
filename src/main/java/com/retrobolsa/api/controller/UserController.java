package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.dto.UserCompetitionHistoryDto;
import com.retrobolsa.api.game.dto.UserProfileResponseDto;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.service.AccountService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final AchievementService achievementService;
    private final AccountService accountService;

    /** O jogador terminou (ou pulou) o passo a passo do primeiro acesso. */
    @PostMapping("/me/onboarded")
    public ResponseEntity<Void> onboarded(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        user.setOnboarded(true);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    /** Troca a senha estando logado (e encerra a obrigação de trocar a senha temporária). */
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordRequest request, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        accountService.changePassword(user, request.senhaAtual(), request.novaSenha(), request.confirmarSenha());
        return ResponseEntity.noContent().build();
    }

    public record PasswordRequest(
            @NotBlank(message = "Digite a senha atual.") String senhaAtual,
            @NotBlank(message = "Digite a nova senha.") @Size(min = 8, message = "A nova senha precisa ter no mínimo 8 caracteres.") String novaSenha,
            @NotBlank(message = "Confirme a nova senha.") String confirmarSenha) {}

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> profile(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        List<Portfolio> portfolios = portfolioRepository.findByUserIdOrderByRankAsc(user.getId());
        Integer bestRank = portfolios.stream()
                .map(Portfolio::getRank)
                .filter(rank -> rank != null && rank > 0)
                .min(Integer::compareTo)
                .orElse(null);

        List<UserCompetitionHistoryDto> history = portfolios.stream()
                .map(p -> UserCompetitionHistoryDto.builder()
                        .roundNumber(p.getCompetition().getRoundNumber())
                        .scenarioTitle(p.getCompetition().getScenarioTitle())
                        .totalReturn(p.getTotalReturn())
                        .finalValue(p.getFinalValue())
                        .rank(p.getRank())
                        .submittedAt(p.getSubmittedAt())
                        .build())
                .sorted(Comparator.comparing(UserCompetitionHistoryDto::getRoundNumber).reversed())
                .toList();

        return ResponseEntity.ok(UserProfileResponseDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .totalScore(user.getTotalScore())
                .bestRank(bestRank)
                .competitions(portfolios.size())
                .history(history)
                .achievements(achievementService.listForUser(user.getId()))
                .onboarded(user.isOnboarded())
                .mustChangePassword(user.isMustChangePassword())
                .build());
    }
}
