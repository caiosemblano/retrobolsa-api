package com.retrobolsa.api.game.practice;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Um treino: a carteira remontada numa rodada já revelada e o resultado que ela teria dado. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "practice_runs")
public class PracticeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "competition_id", nullable = false)
    private UUID competitionId;

    @Column(name = "total_return", nullable = false)
    private BigDecimal totalReturn;

    @Column(name = "final_value", nullable = false)
    private BigDecimal finalValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
