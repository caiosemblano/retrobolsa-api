package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.ArticleResponseDto;
import com.retrobolsa.api.game.education.EducationService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {
    private final EducationService educationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ArticleResponseDto>> list(Authentication authentication) {
        return ResponseEntity.ok(educationService.list(resolveUser(authentication).getId()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable UUID id, Authentication authentication) {
        educationService.complete(resolveUser(authentication).getId(), id);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
