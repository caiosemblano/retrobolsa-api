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
    String title;
    String content;
    int durationMin;
    boolean completed;
}
