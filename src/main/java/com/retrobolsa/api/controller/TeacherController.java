package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.classroom.Classroom;
import com.retrobolsa.api.game.classroom.ClassroomService;
import com.retrobolsa.api.game.classroom.TeacherDashboardService;
import com.retrobolsa.api.game.dto.ClassroomDto;
import com.retrobolsa.api.game.dto.ClassroomStudentDto;
import com.retrobolsa.api.game.dto.QuestionStatDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Área do professor (só TEACHER, pelo SecurityConfig). Cada professor mexe só nas próprias turmas. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/classrooms")
public class TeacherController {

    private final ClassroomService classroomService;
    private final TeacherDashboardService dashboardService;
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

    /** Os alunos da turma, com aulas, quizzes, rodadas, XP e última atividade. */
    @GetMapping("/{id}/students")
    public ResponseEntity<List<ClassroomStudentDto>> students(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(dashboardService.students(teacher(authentication).getId(), id));
    }

    /** As perguntas de quiz que a turma mais erra. */
    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuestionStatDto>> questions(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(dashboardService.mostMissedQuestions(teacher(authentication).getId(), id));
    }

    /** A tabela dos alunos em CSV, para abrir numa planilha. */
    @GetMapping("/{id}/students.csv")
    public ResponseEntity<byte[]> studentsCsv(@PathVariable UUID id, Authentication authentication) {
        UUID teacherId = teacher(authentication).getId();
        Classroom classroom = classroomService.owned(teacherId, id);
        byte[] body = dashboardService.studentsCsv(teacherId, id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("turma-" + slug(classroom.getName()) + ".csv", StandardCharsets.UTF_8)
                        .build().toString())
                .body(body);
    }

    /** "1º ano B" → "1o-ano-b": nome de arquivo sem acentos nem espaços. */
    static String slug(String name) {
        String semAcentos = Normalizer.normalize(name.replace("º", "o").replace("ª", "a"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "alunos" : slug;
    }

    public record CreateRequest(
            @NotBlank(message = "Dê um nome à turma.") @Size(max = 80, message = "O nome da turma tem no máximo 80 caracteres.") String name,
            @Size(max = 120, message = "A instituição tem no máximo 120 caracteres.") String institution) {}

    private User teacher(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
