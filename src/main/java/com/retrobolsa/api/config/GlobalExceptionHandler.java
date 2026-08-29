package com.retrobolsa.api.config;

import com.retrobolsa.api.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(new ErrorResponse(500, ex.getMessage() != null ? ex.getMessage() : "Aconteceu um erro interno..."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(new ErrorResponse(400, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> erros = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            erros.add(Map.of(
                    "campo", fieldError.getField(),
                    "mensagem", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Valor inválido"
            ));
        });

        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            erros.add(Map.of(
                    "campo", globalError.getObjectName(),
                    "mensagem", globalError.getDefaultMessage() != null ? globalError.getDefaultMessage() : "Erro de validação"
            ));
        });

        String firstMessage = !erros.isEmpty() ? erros.get(0).get("mensagem") : "Erro de validação";

        Map<String, Object> body = Map.of(
                "status", 400,
                "erro", firstMessage,
                "detalhes", erros
        );

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
