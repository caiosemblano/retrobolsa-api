package com.retrobolsa.api.game.classroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    Optional<Classroom> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    List<Classroom> findAllByTeacherIdOrderByArchivedAscCreatedAtDesc(UUID teacherId);

    List<Classroom> findAllByIdInAndArchivedFalseOrderByName(Collection<UUID> ids);
}
