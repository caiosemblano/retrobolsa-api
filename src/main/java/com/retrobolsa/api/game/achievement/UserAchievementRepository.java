package com.retrobolsa.api.game.achievement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UserAchievementId> {
    List<UserAchievement> findAllByIdUserId(UUID userId);

    /**
     * Grava o desbloqueio se ainda não existir. O {@code ON CONFLICT} torna a operação
     * atômica: dois eventos simultâneos do mesmo jogador (ex.: duplo clique em
     * "concluir aula") não violam a PK nem derrubam a transação de quem chamou.
     *
     * @return 1 se desbloqueou agora, 0 se o jogador já tinha a conquista
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
            VALUES (:userId, :achievementId, :unlockedAt)
            ON CONFLICT (user_id, achievement_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId,
                       @Param("achievementId") UUID achievementId,
                       @Param("unlockedAt") LocalDateTime unlockedAt);

    @Modifying
    @Query("""
            DELETE FROM UserAchievement ua
            WHERE ua.id.achievementId IN (SELECT a.id FROM Achievement a WHERE a.code IN :codes)
            """)
    int deleteAllByAchievementCodeIn(@Param("codes") Collection<String> codes);
}
