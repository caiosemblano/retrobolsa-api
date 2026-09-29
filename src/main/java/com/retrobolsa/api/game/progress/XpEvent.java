package com.retrobolsa.api.game.progress;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma linha do livro-caixa de XP. Gravada por {@link XpService#award}, nunca alterada, a não ser pelo "visto". */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "xp_events")
public class XpEvent {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private XpSource source;

    @Column(name = "ref_id", nullable = false)
    private String refId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private boolean seen;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
