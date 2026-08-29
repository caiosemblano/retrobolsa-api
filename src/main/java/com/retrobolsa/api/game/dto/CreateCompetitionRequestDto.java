package com.retrobolsa.api.game.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateCompetitionRequestDto {
    @NotNull @Positive
    private Integer roundNumber;
    @NotNull @Positive
    private BigDecimal budget;
    @NotBlank
    private String scenarioTitle;
    private String scenarioDescription;
    @Min(1900)
    private int startYear;
    @Min(1900)
    private int endYear;
    @NotNull
    private LocalDateTime endsAt;
    @NotEmpty
    private List<UUID> assetIds;
}
