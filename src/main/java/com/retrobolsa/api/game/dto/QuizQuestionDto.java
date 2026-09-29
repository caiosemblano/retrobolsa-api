package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Pergunta como o aluno a vê: sem a resposta certa e sem a explicação, que vêm só na correção. */
@Value
@Builder
public class QuizQuestionDto {
    UUID id;
    String prompt;
    List<Option> options;

    @Value
    @Builder
    public static class Option {
        UUID id;
        String text;
    }
}
