package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserProfileResponseDto {
    String username;
    String email;
    String role;
    int totalScore;
    Integer bestRank;
    long competitions;
}
