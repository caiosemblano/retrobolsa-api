package com.retrobolsa.api.exception;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ErrorResponse {
    private int status;
    private String erro;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse(int status, String erro) {
        this.status = status;
        this.erro = erro;
    }
}
