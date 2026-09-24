package com.retrobolsa.api.game.achievement;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "achievements")
public class Achievement {
    @Id
    private UUID id;
    private String code;
    private String title;
    private String description;
    /** "comum", "raro", "epico" ou "lendario" (CHECK no banco). */
    private String rarity;
    @Column(name = "display_order")
    private int displayOrder;
}
