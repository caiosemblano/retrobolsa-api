package com.retrobolsa.api.user.dto;

import lombok.Data;

@Data
public class RegisterRequest{
    private String username;
    private String email;
    private String senha;
}
