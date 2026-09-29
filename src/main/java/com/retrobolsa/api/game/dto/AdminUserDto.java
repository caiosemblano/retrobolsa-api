package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

/** Um usuário na busca do admin, para promover a professor. */
@Value
@Builder
public class AdminUserDto {
    String id;
    String username;
    String email;
    String role;
}
