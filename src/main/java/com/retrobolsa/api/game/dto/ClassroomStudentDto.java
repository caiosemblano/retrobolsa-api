package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Um aluno no painel do professor. Só o username: o e-mail nunca sai para o professor. */
@Value
@Builder
public class ClassroomStudentDto {
    String username;
    LocalDateTime joinedAt;
    long lessonsCompleted;
    /** Quantos quizzes diferentes o aluno já fez. */
    long quizzesTaken;
    /** Média da melhor nota em cada quiz feito, em %; nula se ainda não fez nenhum. */
    BigDecimal quizAverage;
    long roundsPlayed;
    int xp;
    int level;
    String levelTitle;
    /** A última vez que estudou ou jogou; nula se nunca fez nada. */
    LocalDateTime lastActivity;
}
