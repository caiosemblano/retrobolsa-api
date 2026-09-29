package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** A correção: acertos, se passou (e com isso concluiu a aula) e a explicação de cada pergunta. */
@Value
@Builder
public class QuizResultDto {
    int score;
    int total;
    /** Acertou o mínimo para concluir a aula (2 de 3). */
    boolean passed;
    List<QuestionResult> results;

    @Value
    @Builder
    public static class QuestionResult {
        UUID questionId;
        UUID selectedOptionId;
        UUID correctOptionId;
        boolean correct;
        String explanation;
    }
}
