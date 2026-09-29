package com.retrobolsa.api.game.mission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionTemplateRepository extends JpaRepository<MissionTemplate, String> {

    List<MissionTemplate> findAllByOrderByCodeAsc();
}
