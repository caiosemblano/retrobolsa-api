package com.retrobolsa.api.exception;

/** Algo que não existe para quem pediu (ou que ele não pode ver): vira 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
