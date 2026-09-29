package com.retrobolsa.api.game.macro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MacroIndicatorRepository extends JpaRepository<MacroIndicator, Integer> {

    List<MacroIndicator> findAllByYearBetweenOrderByYearAsc(int from, int to);
}
