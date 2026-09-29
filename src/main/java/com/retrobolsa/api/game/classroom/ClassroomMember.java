package com.retrobolsa.api.game.classroom;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/** Um aluno numa turma. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "classroom_members")
public class ClassroomMember {

    @EmbeddedId
    private Id id;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "classroom_id")
        private UUID classroomId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
