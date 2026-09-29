package com.retrobolsa.api.controller;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Monta o corpo de POST /api/articles/{id}/quiz a partir do gabarito do seed. */
final class QuizRespostas {

    private QuizRespostas() {
    }

    /** Todas as perguntas da aula respondidas certo. */
    static String certas(JdbcTemplate jdbc, String articleId) {
        return comAcertos(jdbc, articleId, Integer.MAX_VALUE);
    }

    /**
     * As primeiras {@code acertos} perguntas (na ordem da aula) respondidas certo,
     * e as demais com uma alternativa errada.
     */
    static String comAcertos(JdbcTemplate jdbc, String articleId, int acertos) {
        List<Map<String, Object>> perguntas = jdbc.queryForList(
                "SELECT id FROM quiz_questions WHERE article_id = ?::uuid ORDER BY display_order", articleId);
        StringBuilder corpo = new StringBuilder("{\"answers\":[");
        for (int i = 0; i < perguntas.size(); i++) {
            Object pergunta = perguntas.get(i).get("id");
            Object alternativa = jdbc.queryForObject(
                    "SELECT id FROM quiz_options WHERE question_id = ?::uuid AND correct = ? ORDER BY display_order LIMIT 1",
                    Object.class, pergunta.toString(), i < acertos);
            if (i > 0) corpo.append(',');
            corpo.append("{\"questionId\":\"").append(pergunta).append("\",\"optionId\":\"").append(alternativa).append("\"}");
        }
        return corpo.append("]}").toString();
    }

    /** Monta o corpo com pares (pergunta, alternativa) escolhidos à mão. */
    static String de(List<String[]> pares) {
        return pares.stream()
                .map(p -> "{\"questionId\":\"" + p[0] + "\",\"optionId\":\"" + p[1] + "\"}")
                .collect(Collectors.joining(",", "{\"answers\":[", "]}"));
    }
}
