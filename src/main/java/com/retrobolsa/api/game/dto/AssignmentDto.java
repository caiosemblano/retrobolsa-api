package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/** Uma tarefa da turma como o professor a acompanha: a situação de cada aluno. */
@Value
@Builder
public class AssignmentDto {
    String id;
    String articleId;
    String moduleId;
    String articleTitle;
    LocalDateTime dueAt;
    LocalDateTime createdAt;
    long done;
    long late;
    long pending;
    List<StudentStatus> students;

    @Value
    @Builder
    public static class StudentStatus {
        String username;
        /** FEITA, ATRASADA ou PENDENTE. */
        String status;
        LocalDateTime completedAt;
    }
}
