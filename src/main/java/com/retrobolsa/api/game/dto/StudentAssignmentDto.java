package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** Uma tarefa pendente do aluno: a aula, de qual turma e até quando. */
@Value
@Builder
public class StudentAssignmentDto {
    String id;
    String classroomName;
    String articleId;
    String moduleId;
    String articleTitle;
    LocalDateTime dueAt;
    /** O prazo já passou. */
    boolean late;
}
