package com.retrobolsa.api.game.quiz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizServiceTest {

    @Test
    void passaComDoisTercosOuMais() {
        assertThat(QuizService.passed(2, 3)).isTrue();
        assertThat(QuizService.passed(3, 3)).isTrue();
        assertThat(QuizService.passed(1, 3)).isFalse();
        assertThat(QuizService.passed(0, 3)).isFalse();
        // Quizzes de outro tamanho, se surgirem: 4 de 6 passa, 3 de 5 (60%) não.
        assertThat(QuizService.passed(4, 6)).isTrue();
        assertThat(QuizService.passed(3, 5)).isFalse();
    }

    @Test
    void quizSemPerguntasNuncaAprova() {
        assertThat(QuizService.passed(0, 0)).isFalse();
    }
}
