package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** Uma turma como o aluno a vê: sem o código, com o nome do professor. */
@Value
@Builder
public class StudentClassroomDto {
    String id;
    String name;
    String institution;
    String teacherUsername;
    long memberCount;
    LocalDateTime joinedAt;
}
