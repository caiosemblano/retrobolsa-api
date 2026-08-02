package com.retrobolsa.api.game.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitPortfolioResponseDto {
    private String message;
    private List<String> warnings;
}
