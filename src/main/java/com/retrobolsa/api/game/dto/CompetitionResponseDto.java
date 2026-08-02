package com.retrobolsa.api.game.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionResponseDto {
    private String id;
    private int round;
    private String status;
    private Integer daysLeft;
    private BigDecimal budget;
    private String scenarioTitle;
    private String scenarioDescription;
    private int startYear;
    private int endYear;
    private List<AssetDto> assets;
}
