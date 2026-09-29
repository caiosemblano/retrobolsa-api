package com.retrobolsa.api.game.mission;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MissionsTest {

    @Test
    void semanaIsoDeSegundaADomingo() {
        assertThat(Missions.isoWeek(LocalDate.of(2026, 9, 28))).isEqualTo("2026-W40"); // segunda
        assertThat(Missions.isoWeek(LocalDate.of(2026, 10, 4))).isEqualTo("2026-W40"); // domingo
        assertThat(Missions.isoWeek(LocalDate.of(2026, 10, 5))).isEqualTo("2026-W41");
        // A virada do ano segue a semana ISO: 1º de janeiro de 2027 ainda é da semana 53 de 2026.
        assertThat(Missions.isoWeek(LocalDate.of(2027, 1, 1))).isEqualTo("2026-W53");
        assertThat(Missions.weekEnd(LocalDate.of(2026, 9, 30))).isEqualTo(LocalDateTime.of(2026, 10, 4, 23, 59, 59));
    }

    @Test
    void sorteioDeterministicoComTresMissoesDiferentes() {
        List<String> a = Missions.draw(Missions.CODES, "2026-W40");
        List<String> b = Missions.draw(Missions.CODES, "2026-W40");
        assertThat(a).isEqualTo(b).hasSize(3).doesNotHaveDuplicates();
        assertThat(Missions.CODES).containsAll(a);
    }

    @Test
    void semanasDiferentesVariamAsMissoes() {
        Set<List<String>> sorteios = new HashSet<>();
        for (int semana = 1; semana <= 20; semana++) {
            sorteios.add(Missions.draw(Missions.CODES, String.format("2026-W%02d", semana)));
        }
        assertThat(sorteios.size()).isGreaterThan(5);
    }
}
