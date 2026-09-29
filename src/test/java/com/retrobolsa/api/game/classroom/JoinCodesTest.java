package com.retrobolsa.api.game.classroom;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCodesTest {

    @Test
    void alfabetoSemOsCaracteresQueSeConfundem() {
        assertThat(JoinCodes.ALPHABET).doesNotContain("0", "O", "1", "I");
        for (int i = 0; i < 200; i++) {
            assertThat(JoinCodes.random()).hasSize(6).matches("[" + JoinCodes.ALPHABET + "]+");
        }
    }

    @Test
    void normalizaComoOAlunoDigita() {
        assertThat(JoinCodes.normalize(" ab3-k 9z ")).isEqualTo("AB3K9Z");
        assertThat(JoinCodes.normalize(null)).isEmpty();
    }
}
