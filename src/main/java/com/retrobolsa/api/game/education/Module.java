package com.retrobolsa.api.game.education;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "modules")
public class Module {
    @Id
    private UUID id;
    private String title;
    private String description;
    private String icon;
    @Column(name = "display_order")
    private int displayOrder;
}
