package com.retrobolsa.api.game.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private LocalDateTime endsAt;
    /** Selic, inflação, dólar e PIB do ano anterior ao início: o que o investidor sabia na época. */
    private List<EconomicIndicatorDto> economicIndicators;
    private List<AssetDto> assets;
}
