package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationDto {
    String id;
    /** RODADA_ABERTA, RESULTADO_REVELADO, TAREFA_NOVA ou MISSAO_CUMPRIDA. */
    String type;
    String title;
    String body;
    /** Endereço do app que a notificação abre. */
    String link;
    boolean read;
    LocalDateTime createdAt;
}
