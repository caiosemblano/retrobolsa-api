package com.retrobolsa.api.user.dto;

import com.retrobolsa.api.validation.SenhasIguais;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@SenhasIguais
public class RegisterRequest{

    @NotBlank(message = "Preencha o nome de usuário!")
    private String username;

    @NotBlank(message = "Preencha o campo de email!")
    @Email(message = "Formato de email inválido!")
    private String email;

    @NotBlank(message = "Insira a senha!")
    @Size(min = 8, message = "Senha precisa ter no mínimo 8 caracteres")
    private String senha;

    @NotBlank(message = "Confirmação de senha necessário!!!")
    private String confirmarSenha;

    /**
     * Marcou "Tenho 18 anos ou mais, ou tenho autorização do meu responsável". O app
     * web exige a caixa; aqui é opcional para não quebrar clientes mais antigos, e o
     * momento do aceite fica gravado quando vem.
     */
    private Boolean aceiteTermos;
}


