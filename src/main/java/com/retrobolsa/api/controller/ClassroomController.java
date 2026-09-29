package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.classroom.ClassroomService;
import com.retrobolsa.api.game.dto.StudentClassroomDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** O lado do aluno: entrar com o código, ver as próprias turmas e sair quando quiser. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;
    private final UserRepository userRepository;

    @GetMapping("/mine")
    public ResponseEntity<List<StudentClassroomDto>> mine(Authentication authentication) {
        return ResponseEntity.ok(classroomService.mine(user(authentication).getId()));
    }

    @PostMapping("/join")
    public ResponseEntity<StudentClassroomDto> join(@Valid @RequestBody JoinRequest request, Authentication authentication) {
        return ResponseEntity.ok(classroomService.join(user(authentication).getId(), request.code()));
    }

    @DeleteMapping("/{id}/membership")
    public ResponseEntity<Void> leave(@PathVariable UUID id, Authentication authentication) {
        classroomService.leave(user(authentication).getId(), id);
        return ResponseEntity.noContent().build();
    }

    public record JoinRequest(@NotBlank(message = "Digite o código da turma.") String code) {}

    private User user(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
