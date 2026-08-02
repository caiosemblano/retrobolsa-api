package com.retrobolsa.api.game.asset;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "anonymous_name", nullable = false)
    private String anonymousName;

    @Column(name = "real_name")
    private String realName;

    @Column
    private String ticker;

    @Column(nullable = false)
    private String type;

    @Column
    private String sector;

    @Column(name = "bond_type")
    private String bondType;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
