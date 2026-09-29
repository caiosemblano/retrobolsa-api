package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** Uma turma como o professor a vê: com o código para projetar em sala. */
@Value
@Builder
public class ClassroomDto {
    String id;
    String name;
    String institution;
    String joinCode;
    boolean archived;
    long memberCount;
    LocalDateTime createdAt;
}
