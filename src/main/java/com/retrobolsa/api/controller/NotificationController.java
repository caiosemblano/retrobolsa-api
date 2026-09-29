package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.NotificationDto;
import com.retrobolsa.api.game.notification.NotificationService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /** As notificações do usuário, das mais novas para as mais antigas, paginadas (base 0). */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(@RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size,
                                                      Authentication authentication) {
        return ResponseEntity.ok(notificationService.list(resolveUser(authentication).getId(), page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(resolveUser(authentication).getId())));
    }

    /** Marca como lidas: as informadas em "ids" ou, sem ids, todas. */
    @PostMapping("/read")
    public ResponseEntity<Void> read(@RequestBody(required = false) ReadRequest request, Authentication authentication) {
        notificationService.markRead(resolveUser(authentication).getId(), request == null ? null : request.ids());
        return ResponseEntity.noContent().build();
    }

    public record ReadRequest(List<UUID> ids) {}

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
