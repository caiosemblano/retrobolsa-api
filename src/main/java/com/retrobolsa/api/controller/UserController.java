package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.UserProfileResponseDto;
import com.retrobolsa.api.game.portfolio.Portfolio;
import com.retrobolsa.api.game.portfolio.PortfolioRepository;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

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
        return ResponseEntity.ok(UserProfileResponseDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .totalScore(user.getTotalScore())
                .bestRank(bestRank)
                .competitions(portfolios.size())
                .build());
    }
}
