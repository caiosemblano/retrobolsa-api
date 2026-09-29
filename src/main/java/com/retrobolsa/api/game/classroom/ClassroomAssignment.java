package com.retrobolsa.api.game.classroom;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma aula que o professor passou para a turma, com prazo. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "classroom_assignments")
public class ClassroomAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "classroom_id", nullable = false)
    private UUID classroomId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
