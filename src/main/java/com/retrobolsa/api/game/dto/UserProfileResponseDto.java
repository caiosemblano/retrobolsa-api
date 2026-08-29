package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class UserProfileResponseDto {
    String username;
    String email;
    String role;
    int totalScore;
    Integer bestRank;
    long competitions;
    List<UserCompetitionHistoryDto> history;
}
