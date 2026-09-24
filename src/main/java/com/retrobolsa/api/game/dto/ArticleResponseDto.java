package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ArticleResponseDto {
    UUID id;
    UUID moduleId;
    String moduleTitle;
    String moduleDescription;
    /** Nome de ícone lucide em kebab-case, como gravado no seed (ex.: "trending-up"). */
    String moduleIcon;
    String title;
    String content;
    int durationMin;
    int displayOrder;
    /** ID do vídeo no YouTube; null se a aula não tem vídeo. */
    String videoId;
    boolean completed;
}
