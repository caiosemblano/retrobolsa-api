package com.retrobolsa.api.config;

import com.retrobolsa.api.exception.ErrorResponse;
import com.retrobolsa.api.exception.RateLimitExceededException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parametro invalido: " + ex.getName();
        return new ResponseEntity<>(new ErrorResponse(400, message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(new ErrorResponse(409, "Conflito ao salvar os dados. A operacao ja pode ter sido realizada."), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        return new ResponseEntity<>(new ErrorResponse(429, ex.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
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
