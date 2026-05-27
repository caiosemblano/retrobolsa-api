package com.retrobolsa.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @Email(message = "Formato de email inválido.")
    @NotBlank(message = "Favor, preencha o campo de email.")
    private String email;

    @NotBlank(message = "Favor, preencher o campo de senha.")
    @Size(min = 8, max = 20, message = "Senha precisa ter no mínimo 8 caracteres")
    private String senha;
}
