package com.retrobolsa.api.game.achievement;

import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    /** Contas administrativas não são jogadores: não ganham conquistas, como já não entram nos rankings. */
    private static final String ADMIN_ROLE = "ADMIN";

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    /**
     * Desbloqueia as conquistas informadas, ignorando as que o usuário já tem.
     * Idempotente: repetir a chamada com os mesmos códigos não altera nada, então
     * os gatilhos podem reavaliar tudo a cada evento sem guardar estado próprio.
     *
     * @return os códigos desbloqueados nesta chamada (vazio se nenhum era novo)
     * @throws IllegalStateException se algum código não existir no catálogo — é erro
     *                               de programação (constante sem linha no seed), não do usuário
     */
    @Transactional
    public List<String> unlock(User user, Collection<String> codes) {
        if (codes.isEmpty() || ADMIN_ROLE.equals(user.getRole())) {
            return List.of();
        }

        Set<String> requested = new LinkedHashSet<>(codes);
        List<Achievement> achievements = achievementRepository.findAllByCodeIn(requested);
        if (achievements.size() != requested.size()) {
            Set<String> found = achievements.stream().map(Achievement::getCode).collect(Collectors.toSet());
            requested.removeAll(found);
            throw new IllegalStateException("Conquista ausente do catálogo: " + requested);
        }

        Set<UUID> alreadyUnlocked = userAchievementRepository.findAllByIdUserId(user.getId()).stream()
                .map(unlocked -> unlocked.getId().getAchievementId())
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        List<UserAchievement> toSave = new ArrayList<>();
        List<String> newlyUnlocked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (alreadyUnlocked.contains(achievement.getId())) {
                continue;
            }
            UserAchievement unlocked = new UserAchievement();
            unlocked.setId(new UserAchievementId(user.getId(), achievement.getId()));
            unlocked.setUnlockedAt(now);
            toSave.add(unlocked);
            newlyUnlocked.add(achievement.getCode());
        }
        userAchievementRepository.saveAll(toSave);
        return newlyUnlocked;
    }

    /**
     * Todas as conquistas do catálogo, na ordem de exibição, marcando as que o
     * usuário já desbloqueou. Duas queries no total, independente do tamanho do catálogo.
     */
    @Transactional(readOnly = true)
    public List<AchievementResponseDto> listForUser(UUID userId) {
        Map<UUID, LocalDateTime> unlockedAt = userAchievementRepository.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(
                        unlocked -> unlocked.getId().getAchievementId(),
                        UserAchievement::getUnlockedAt));

        return achievementRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(achievement -> AchievementResponseDto.builder()
                        .code(achievement.getCode())
                        .title(achievement.getTitle())
                        .description(achievement.getDescription())
                        .rarity(achievement.getRarity())
                        .unlocked(unlockedAt.containsKey(achievement.getId()))
                        .unlockedAt(unlockedAt.get(achievement.getId()))
                        .build())
                .toList();
    }
}
