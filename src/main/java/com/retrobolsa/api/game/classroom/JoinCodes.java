package com.retrobolsa.api.game.classroom;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Códigos de turma: 6 caracteres, sem os que se confundem ao copiar da lousa
 * (0 e O, 1 e I). O aluno pode digitar em minúsculas ou com espaços.
 */
public final class JoinCodes {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodes() {}

    public static String random() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** Como o aluno digitou → como está no banco: maiúsculas, sem espaços nem hífens. */
    public static String normalize(String typed) {
        return typed == null ? "" : typed.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
