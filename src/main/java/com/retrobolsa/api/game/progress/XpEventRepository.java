package com.retrobolsa.api.game.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {

    /**
     * Grava o evento se ele ainda não existe. Atômico no banco (ON CONFLICT):
     * duas requisições simultâneas pela mesma aula não dão XP duas vezes.
     *
     * @return 1 se gravou, 0 se o evento já existia
     */
    @Modifying
    @Query(value = """
            INSERT INTO xp_events (id, user_id, source, ref_id, amount, seen, created_at)
            VALUES (gen_random_uuid(), :userId, :source, :refId, :amount, FALSE, :createdAt)
            ON CONFLICT (user_id, source, ref_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("source") String source, @Param("refId") String refId,
                       @Param("amount") int amount, @Param("createdAt") LocalDateTime createdAt);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM XpEvent e WHERE e.userId = :userId")
    int totalXp(@Param("userId") UUID userId);

    @Query("SELECT e.createdAt FROM XpEvent e WHERE e.userId = :userId")
    List<LocalDateTime> activityTimes(@Param("userId") UUID userId);

    List<XpEvent> findAllByUserIdAndSeenFalseOrderByCreatedAtAsc(UUID userId);

    @Modifying
    @Query("UPDATE XpEvent e SET e.seen = TRUE WHERE e.userId = :userId AND e.id IN :ids")
    int markSeen(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** Reset do jogo: some o XP de carteiras, rodadas e das conquistas de jogo, que deixam de existir. */
    @Modifying
    @Query("""
            DELETE FROM XpEvent e WHERE e.source IN :sources
               OR (e.source = com.retrobolsa.api.game.progress.XpSource.ACHIEVEMENT AND e.refId IN :achievementCodes)
            """)
    int deleteGameEvents(@Param("sources") Collection<XpSource> sources,
                         @Param("achievementCodes") Collection<String> achievementCodes);
}
