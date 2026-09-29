package com.retrobolsa.api.game.quiz;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/** A resposta dada a uma pergunta numa tentativa; base das "perguntas que a turma mais erra". */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_quiz_answers")
public class UserQuizAnswer {

    @EmbeddedId
    private Id id;

    @MapsId("attemptId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private UserQuizAttempt attempt;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(nullable = false)
    private boolean correct;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        private UUID attemptId;
        @Column(name = "question_id")
        private UUID questionId;
    }
}
