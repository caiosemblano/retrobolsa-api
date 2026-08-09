package com.retrobolsa.api.game.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitPortfolioRequestDto {

    @NotNull(message = "competitionId e obrigatorio")
    private String competitionId;

    @NotEmpty(message = "A lista de alocacoes nao pode estar vazia")
    @Valid
    private List<AllocationRequestDto> allocations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationRequestDto {
        @NotNull(message = "assetId e obrigatorio")
        private String assetId;

        @NotNull(message = "amount e obrigatorio")
        private BigDecimal amount;
    }
}
