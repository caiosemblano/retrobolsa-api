package com.retrobolsa.api.game.classroom;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherDashboardCsvTest {

    @Test
    void celulaComSeparadorOuAspasVaiEntreAspas() {
        assertThat(TeacherDashboardService.cell("ana")).isEqualTo("ana");
        assertThat(TeacherDashboardService.cell("ana;beto")).isEqualTo("\"ana;beto\"");
        assertThat(TeacherDashboardService.cell("o \"craque\"")).isEqualTo("\"o \"\"craque\"\"\"");
        assertThat(TeacherDashboardService.cell(null)).isEmpty();
    }
}
