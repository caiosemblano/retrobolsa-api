package com.retrobolsa.api.game.progress;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O que os gatilhos do jogo chamam para recompensar: dá o XP (uma vez só) e, se
 * ele foi dado agora, avalia as conquistas que dependem de XP (nível e constância).
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final XpService xpService;
    private final AchievementService achievementService;

    @Transactional
    public void reward(User user, XpSource source, String refId, int amount) {
        if (xpService.award(user, source, refId, amount)) {
            achievementService.evaluateProgress(user);
        }
    }
}
