package com.retrobolsa.api.game.mission;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Um modelo de missão semanal (seed da V24). */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "mission_templates")
public class MissionTemplate {

    @Id
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionEvent event;

    @Column(nullable = false)
    private int target;

    @Column(nullable = false)
    private int xp;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
