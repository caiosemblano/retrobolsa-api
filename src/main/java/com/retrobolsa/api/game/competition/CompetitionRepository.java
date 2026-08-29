package com.retrobolsa.api.game.competition;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

public interface CompetitionRepository extends JpaRepository<Competition, UUID> {

    Optional<Competition> findByStatus(String status);
    List<Competition> findAllByStatusAndEndsAtLessThanEqual(String status, LocalDateTime endsAt);
    List<Competition> findAllByOrderByRoundNumberAsc();
}
