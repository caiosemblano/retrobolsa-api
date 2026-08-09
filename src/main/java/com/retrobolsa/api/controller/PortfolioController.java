package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.PortfolioResultDto;
import com.retrobolsa.api.game.dto.SubmitPortfolioRequestDto;
import com.retrobolsa.api.game.dto.SubmitPortfolioResponseDto;
import com.retrobolsa.api.game.portfolio.PortfolioService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SubmitPortfolioResponseDto> submit(
            @Valid @RequestBody SubmitPortfolioRequestDto request,
            Authentication authentication) {

        User user = resolveUser(authentication);
        SubmitPortfolioResponseDto response = portfolioService.submit(user.getId(), request);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/my-last-result")
    public ResponseEntity<PortfolioResultDto> getLastResult(Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(portfolioService.getLastResult(user.getId()));
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
