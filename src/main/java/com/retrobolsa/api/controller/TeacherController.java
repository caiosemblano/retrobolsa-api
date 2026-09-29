package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.classroom.ClassroomService;
import com.retrobolsa.api.game.dto.ClassroomDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Área do professor (só TEACHER, pelo SecurityConfig). Cada professor mexe só nas próprias turmas. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/classrooms")
public class TeacherController {

    private final ClassroomService classroomService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ClassroomDto>> list(Authentication authentication) {
        return ResponseEntity.ok(classroomService.teacherClassrooms(teacher(authentication).getId()));
    }

    @PostMapping
    public ResponseEntity<ClassroomDto> create(@Valid @RequestBody CreateRequest request, Authentication authentication) {
        return ResponseEntity.status(201)
                .body(classroomService.create(teacher(authentication).getId(), request.name(), request.institution()));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ClassroomDto> archive(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(classroomService.setArchived(teacher(authentication).getId(), id, true));
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<ClassroomDto> unarchive(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(classroomService.setArchived(teacher(authentication).getId(), id, false));
    }

    @PostMapping("/{id}/code")
    public ResponseEntity<ClassroomDto> regenerateCode(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(classroomService.regenerateCode(teacher(authentication).getId(), id));
    }

    public record CreateRequest(
            @NotBlank(message = "Dê um nome à turma.") @Size(max = 80, message = "O nome da turma tem no máximo 80 caracteres.") String name,
            @Size(max = 120, message = "A instituição tem no máximo 120 caracteres.") String institution) {}

    private User teacher(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
