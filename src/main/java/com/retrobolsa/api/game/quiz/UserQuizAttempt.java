package com.retrobolsa.api.game.quiz;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Uma tentativa de quiz. Todas ficam guardadas: a melhor nota e o histórico saem daqui. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_quiz_attempts")
public class UserQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserQuizAnswer> answers = new ArrayList<>();
}
