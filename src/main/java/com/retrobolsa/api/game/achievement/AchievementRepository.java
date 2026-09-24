package com.retrobolsa.api.game.achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    List<Achievement> findAllByOrderByDisplayOrderAsc();

    List<Achievement> findAllByCodeIn(Collection<String> codes);
}
