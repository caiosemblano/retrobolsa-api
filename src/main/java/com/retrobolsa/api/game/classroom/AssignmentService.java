package com.retrobolsa.api.game.classroom;

import com.retrobolsa.api.exception.NotFoundException;
import com.retrobolsa.api.game.dto.AssignmentDto;
import com.retrobolsa.api.game.dto.StudentAssignmentDto;
import com.retrobolsa.api.game.education.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Tarefas da turma: o professor passa uma aula com prazo e acompanha quem já fez;
 * o aluno vê o que falta. "Feita" é ter concluído a aula, a qualquer tempo.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    public static final String DONE = "FEITA";
    public static final String LATE = "ATRASADA";
    public static final String PENDING = "PENDENTE";

    private final ClassroomService classroomService;
    private final ClassroomAssignmentRepository assignmentRepository;
    private final ArticleRepository articleRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    @Transactional
    public ClassroomAssignment create(UUID teacherId, UUID classroomId, UUID articleId, LocalDateTime dueAt) {
        Classroom classroom = classroomService.owned(teacherId, classroomId);
        if (classroom.isArchived()) {
            throw new IllegalArgumentException("Desarquive a turma para passar tarefas.");
        }
        if (!articleRepository.existsById(articleId)) {
            throw new IllegalArgumentException("Aula não encontrada.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!dueAt.isAfter(now)) {
            throw new IllegalArgumentException("O prazo precisa ser depois de agora.");
        }
        if (assignmentRepository.existsByClassroomIdAndArticleId(classroomId, articleId)) {
            throw new IllegalArgumentException("Esta aula já é uma tarefa da turma.");
        }
        return assignmentRepository.save(ClassroomAssignment.builder()
                .classroomId(classroomId).articleId(articleId).dueAt(dueAt).createdAt(now).build());
    }

    @Transactional
    public void delete(UUID teacherId, UUID classroomId, UUID assignmentId) {
        classroomService.owned(teacherId, classroomId);
        ClassroomAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getClassroomId().equals(classroomId))
                .orElseThrow(() -> new NotFoundException("Tarefa não encontrada"));
        assignmentRepository.delete(assignment);
    }

    /** As tarefas da turma, da mais recente para a mais antiga, com a situação de cada aluno. */
    @Transactional(readOnly = true)
    public List<AssignmentDto> forTeacher(UUID teacherId, UUID classroomId) {
        classroomService.owned(teacherId, classroomId);
        LocalDateTime now = LocalDateTime.now(clock);
        Map<String, Object> params = Map.of("classroomId", classroomId);

        Map<UUID, List<AssignmentDto.StudentStatus>> statusByAssignment = new HashMap<>();
        jdbc.query("""
                SELECT a.id AS assignment_id, a.due_at, u.username, p.completed_at
                FROM classroom_assignments a
                JOIN classroom_members m ON m.classroom_id = a.classroom_id
                JOIN users u ON u.id = m.user_id
                LEFT JOIN user_article_progress p ON p.user_id = u.id AND p.article_id = a.article_id
                WHERE a.classroom_id = :classroomId
                ORDER BY LOWER(u.username)
                """, params, rs -> {
            LocalDateTime completedAt = toLocal(rs.getTimestamp("completed_at"));
            LocalDateTime dueAt = toLocal(rs.getTimestamp("due_at"));
            statusByAssignment.computeIfAbsent(rs.getObject("assignment_id", UUID.class), id -> new ArrayList<>())
                    .add(AssignmentDto.StudentStatus.builder()
                            .username(rs.getString("username"))
                            .status(status(completedAt, dueAt, now))
                            .completedAt(completedAt)
                            .build());
        });

        return jdbc.query("""
                SELECT a.id, a.article_id, ar.module_id, ar.title, a.due_at, a.created_at
                FROM classroom_assignments a JOIN articles ar ON ar.id = a.article_id
                WHERE a.classroom_id = :classroomId
                ORDER BY a.created_at DESC
                """, params, (rs, i) -> {
            UUID id = rs.getObject("id", UUID.class);
            List<AssignmentDto.StudentStatus> students = statusByAssignment.getOrDefault(id, List.of());
            return AssignmentDto.builder()
                    .id(id.toString())
                    .articleId(rs.getObject("article_id", UUID.class).toString())
                    .moduleId(rs.getObject("module_id", UUID.class).toString())
                    .articleTitle(rs.getString("title"))
                    .dueAt(toLocal(rs.getTimestamp("due_at")))
                    .createdAt(toLocal(rs.getTimestamp("created_at")))
                    .done(count(students, DONE))
                    .late(count(students, LATE))
                    .pending(count(students, PENDING))
                    .students(students)
                    .build();
        });
    }

    /** O que falta o aluno fazer, nas turmas ativas dele, do prazo mais próximo ao mais distante. */
    @Transactional(readOnly = true)
    public List<StudentAssignmentDto> pendingFor(UUID userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return jdbc.query("""
                SELECT a.id, c.name AS classroom_name, a.article_id, ar.module_id, ar.title, a.due_at
                FROM classroom_assignments a
                JOIN classrooms c ON c.id = a.classroom_id AND NOT c.archived
                JOIN classroom_members m ON m.classroom_id = a.classroom_id AND m.user_id = :userId
                JOIN articles ar ON ar.id = a.article_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM user_article_progress p WHERE p.user_id = :userId AND p.article_id = a.article_id)
                ORDER BY a.due_at, ar.title
                """, Map.of("userId", userId), (rs, i) -> {
            LocalDateTime dueAt = toLocal(rs.getTimestamp("due_at"));
            return StudentAssignmentDto.builder()
                    .id(rs.getObject("id", UUID.class).toString())
                    .classroomName(rs.getString("classroom_name"))
                    .articleId(rs.getObject("article_id", UUID.class).toString())
                    .moduleId(rs.getObject("module_id", UUID.class).toString())
                    .articleTitle(rs.getString("title"))
                    .dueAt(dueAt)
                    .late(now.isAfter(dueAt))
                    .build();
        });
    }

    static String status(LocalDateTime completedAt, LocalDateTime dueAt, LocalDateTime now) {
        if (completedAt != null) return DONE;
        return now.isAfter(dueAt) ? LATE : PENDING;
    }

    private static long count(List<AssignmentDto.StudentStatus> students, String status) {
        return students.stream().filter(s -> s.getStatus().equals(status)).count();
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
