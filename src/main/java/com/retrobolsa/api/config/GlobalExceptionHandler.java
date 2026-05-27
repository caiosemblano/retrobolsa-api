package com.retrobolsa.api.config;

import com.retrobolsa.api.exception.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //Exceção geralzona (Pau pra toda obra)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(new ErrorResponse(500, "Aconteceu um erro interno..."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(new ErrorResponse(400, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    /**
     * Formata erros de validação de Bean Validation (ex: campo obrigatório ausente, email inválido).
     * Retorna uma lista estruturada [{campo: "email", mensagem: "Formato inválido"}]
     * em vez da mensagem bruta do Spring, que é extensa e expõe detalhes internos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> Map.of(
                        "campo", fieldError.getField(),
                        "mensagem", fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valor inválido"
                ))
                .collect(Collectors.toList());

        Map<String, Object> body = Map.of(
                "status", 400,
                "erro", "Erro de validação",
                "detalhes", erros
        );

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}

