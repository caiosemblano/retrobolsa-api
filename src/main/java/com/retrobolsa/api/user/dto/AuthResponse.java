package com.retrobolsa.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de resposta ao login bem-sucedido.
 * Encapsula o token JWT em um objeto JSON estruturado para facilitar
 * o consumo pelo frontend (Web e Mobile).
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    /** Token JWT gerado após autenticação bem-sucedida. */
    private String token;

    /** Tipo do token — sempre "Bearer" para uso no cabeçalho Authorization. */
    private String type;

    /** Tempo de expiração do token em milissegundos a partir da emissão. */
    private long expiresIn;
}
