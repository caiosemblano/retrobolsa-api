package com.retrobolsa.api.game.classroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClassroomAssignmentRepository extends JpaRepository<ClassroomAssignment, UUID> {

    boolean existsByClassroomIdAndArticleId(UUID classroomId, UUID articleId);
}
