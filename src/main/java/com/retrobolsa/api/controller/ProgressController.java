package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.ProgressDto;
import com.retrobolsa.api.game.dto.ProgressNewsDto;
import com.retrobolsa.api.game.progress.ProgressNewsService;
import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/progress")
public class ProgressController {

    private final XpService xpService;
    private final ProgressNewsService newsService;
    private final UserRepository userRepository;

    /** XP, nível e semanas seguidas do jogador logado. */
    @GetMapping
    public ResponseEntity<ProgressDto> progress(Authentication authentication) {
        return ResponseEntity.ok(xpService.summary(resolveUser(authentication).getId()));
    }

    /** Ganhos ainda não comemorados no app. */
    @GetMapping("/news")
    public ResponseEntity<ProgressNewsDto> news(Authentication authentication) {
        return ResponseEntity.ok(newsService.news(resolveUser(authentication).getId()));
    }

    /** Marca como vistos os ganhos já comemorados (só os do próprio jogador). */
    @PostMapping("/news/ack")
    public ResponseEntity<Void> acknowledge(@Valid @RequestBody AckRequest request, Authentication authentication) {
        newsService.acknowledge(resolveUser(authentication).getId(), request.ids());
        return ResponseEntity.noContent().build();
    }

    public record AckRequest(@NotNull List<UUID> ids) {}

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
