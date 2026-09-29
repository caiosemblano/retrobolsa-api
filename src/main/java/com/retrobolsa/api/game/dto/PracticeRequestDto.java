package com.retrobolsa.api.game.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** O mesmo corpo do envio de carteira; a rodada vem na URL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeRequestDto {

    @NotEmpty(message = "A lista de alocações não pode estar vazia")
    @Valid
    private List<SubmitPortfolioRequestDto.AllocationRequestDto> allocations;
}
