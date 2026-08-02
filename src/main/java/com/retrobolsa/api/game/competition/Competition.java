package com.retrobolsa.api.game.competition;

import com.retrobolsa.api.game.asset.Asset;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "competitions")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "round_number", nullable = false, unique = true)
    private int roundNumber;

    @Column(nullable = false)
    @Builder.Default
    private String status = "open";

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal budget = new BigDecimal("100000.00");

    @Column(name = "scenario_title")
    private String scenarioTitle;

    @Column(name = "scenario_description", columnDefinition = "TEXT")
    private String scenarioDescription;

    @Column(name = "start_year", nullable = false)
    private int startYear;

    @Column(name = "end_year", nullable = false)
    private int endYear;

    @Column(name = "days_left")
    private Integer daysLeft;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "competition_assets",
        joinColumns = @JoinColumn(name = "competition_id"),
        inverseJoinColumns = @JoinColumn(name = "asset_id")
    )
    private List<Asset> assets;
}
