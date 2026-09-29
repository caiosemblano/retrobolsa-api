package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.MissionWeekDto;
import com.retrobolsa.api.game.mission.MissionService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;
    private final UserRepository userRepository;

    /** As 3 missões da semana e o progresso do jogador. */
    @GetMapping("/week")
    public ResponseEntity<MissionWeekDto> week(Authentication authentication) {
        return ResponseEntity.ok(missionService.week(resolveUser(authentication).getId()));
    }

    /** O app avisa que foi aberto: conta para "volte em 2 dias" (uma vez por dia). */
    @PostMapping("/visit")
    public ResponseEntity<Void> visit(Authentication authentication) {
        missionService.recordVisit(resolveUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
