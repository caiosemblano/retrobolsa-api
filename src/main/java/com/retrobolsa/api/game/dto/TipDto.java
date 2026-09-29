package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

/** Uma lição tirada da carteira do jogador, com a aula que aprofunda o assunto (quando há). */
@Value
@Builder
public class TipDto {
    /** Identificador estável da regra (ex.: PERDEU_PARA_INFLACAO), para o app escolher ícone e testar. */
    String code;
    String message;
    /** Módulo e aula sugeridos; nulos quando ainda não existe aula do assunto. */
    String moduleId;
    String articleId;
}
