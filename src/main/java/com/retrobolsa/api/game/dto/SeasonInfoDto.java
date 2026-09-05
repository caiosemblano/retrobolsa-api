package com.retrobolsa.api.game.dto;

import lombok.*;

/** Limites da temporada atual: número da temporada e faixa de rodadas que a compõe. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonInfoDto {
    private int seasonNumber;
    private int roundStart;
    private int roundEnd;
}
