package com.retrobolsa.api.game.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, ClassroomMember.Id> {

    /** Entra na turma se ainda não está nela. Atômico: dois cliques no "Entrar" não colidem. */
    @Modifying
    @Query(value = """
            INSERT INTO classroom_members (classroom_id, user_id, joined_at)
            VALUES (:classroomId, :userId, :joinedAt)
            ON CONFLICT (classroom_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("classroomId") UUID classroomId, @Param("userId") UUID userId,
                       @Param("joinedAt") LocalDateTime joinedAt);

    List<ClassroomMember> findAllByIdUserId(UUID userId);

    @Query("SELECT m.id.userId FROM ClassroomMember m WHERE m.id.classroomId = :classroomId")
    Set<UUID> memberIds(@Param("classroomId") UUID classroomId);

    @Query("""
            SELECT m.id.classroomId AS classroomId, COUNT(m) AS total FROM ClassroomMember m
            WHERE m.id.classroomId IN :ids GROUP BY m.id.classroomId
            """)
    List<MemberCount> countByClassroomIds(@Param("ids") Collection<UUID> ids);

    interface MemberCount {
        UUID getClassroomId();
        long getTotal();
    }
}
