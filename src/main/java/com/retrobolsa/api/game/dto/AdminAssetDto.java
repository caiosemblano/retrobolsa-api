package com.retrobolsa.api.game.dto;

import lombok.*;

import java.util.List;

/** Ativo como o admin vê ao montar uma rodada: com o nome real e os anos que têm retorno. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAssetDto {
    private String id;
    private String anonymousName;
    private String realName;
    private String ticker;
    private String type;
    private String sector;
    private String bondType;
    /** Anos com annual_return preenchido, em ordem crescente. */
    private List<Integer> years;
}
