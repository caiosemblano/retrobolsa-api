package com.retrobolsa.api.game.progress;

import com.retrobolsa.api.game.dto.ProgressDto;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * XP mede dedicação, não desempenho: estudar e participar sempre fazem subir de
 * nível, inclusive para quem perdeu dinheiro na rodada. O ranking continua medindo
 * a rentabilidade.
 */
@Service
@RequiredArgsConstructor
public class XpService {

    public static final int LESSON_XP = 20;
    public static final int QUIZ_PERFECT_XP = 15;
    public static final int PORTFOLIO_XP = 30;
    public static final int BEAT_CDI_XP = 20;
    /** XP de cada conquista, pela raridade (a mesma que define a cor do emblema). */
    public static final Map<String, Integer> ACHIEVEMENT_XP = Map.of(
            "comum", 10, "raro", 25, "epico", 50, "lendario", 100);

    private static final String ADMIN_ROLE = "ADMIN";

    private final XpEventRepository repository;
    private final Clock clock;

    /**
     * Dá XP uma única vez por (fonte, referência). Idempotente: os gatilhos podem
     * chamar de novo sem risco, como já acontece com as conquistas.
     *
     * @return true se o XP foi dado agora, false se já tinha sido (ou se é ADMIN)
     */
    @Transactional
    public boolean award(User user, XpSource source, String refId, int amount) {
        if (ADMIN_ROLE.equals(user.getRole()) || amount <= 0) return false;
        return repository.insertIfAbsent(user.getId(), source.name(), refId, amount, LocalDateTime.now(clock)) > 0;
    }

    @Transactional(readOnly = true)
    public ProgressDto summary(UUID userId) {
        int xp = repository.totalXp(userId);
        Levels.Level level = Levels.of(xp);
        var next = Levels.next(level);
        List<LocalDate> days = repository.activityTimes(userId).stream().map(LocalDateTime::toLocalDate).toList();
        return ProgressDto.builder()
                .xp(xp)
                .level(level.number())
                .levelTitle(level.title())
                .levelMinXp(level.minXp())
                .nextLevel(next.map(Levels.Level::number).orElse(null))
                .nextLevelTitle(next.map(Levels.Level::title).orElse(null))
                .nextLevelMinXp(next.map(Levels.Level::minXp).orElse(null))
                .streakWeeks(Streaks.currentWeeks(days, LocalDate.now(clock)))
                .build();
    }

    @Transactional(readOnly = true)
    public List<XpEvent> unseen(UUID userId) {
        return repository.findAllByUserIdAndSeenFalseOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void markSeen(UUID userId, Collection<UUID> ids) {
        if (!ids.isEmpty()) repository.markSeen(userId, ids);
    }

    /** Reset do jogo (admin): apaga o XP que vinha de carteiras, rodadas e conquistas de jogo. */
    @Transactional
    public void resetGameXp(Collection<String> gameAchievementCodes) {
        repository.deleteGameEvents(List.of(XpSource.PORTFOLIO, XpSource.BEAT_CDI), gameAchievementCodes);
    }
}
