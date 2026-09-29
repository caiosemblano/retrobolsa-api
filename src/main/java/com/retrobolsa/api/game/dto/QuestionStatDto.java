package com.retrobolsa.api.game.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Uma pergunta de quiz com a taxa de erro dos alunos da turma. */
@Value
@Builder
public class QuestionStatDto {
    String questionId;
    String prompt;
    String articleId;
    String moduleId;
    String articleTitle;
    /** Respostas dos alunos da turma, somando todas as tentativas. */
    long answers;
    long wrong;
    /** Erros sobre respostas, em %. */
    BigDecimal errorRate;
    /** Quantos alunos diferentes responderam. */
    long students;
    /** A alternativa errada mais escolhida pela turma. */
    String commonWrongAnswer;
}
