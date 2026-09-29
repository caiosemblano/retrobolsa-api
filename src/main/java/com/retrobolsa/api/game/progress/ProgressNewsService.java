package com.retrobolsa.api.game.progress;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.competition.Competition;
import com.retrobolsa.api.game.competition.CompetitionRepository;
import com.retrobolsa.api.game.dto.AchievementResponseDto;
import com.retrobolsa.api.game.dto.ProgressNewsDto;
import com.retrobolsa.api.game.education.Article;
import com.retrobolsa.api.game.education.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta a "comemoração": os ganhos de XP ainda não vistos, com um texto legível
 * para cada um, se o jogador subiu de nível e os emblemas das conquistas novas.
 */
@Service
@RequiredArgsConstructor
public class ProgressNewsService {

    private final XpService xpService;
    private final AchievementService achievementService;
    private final ArticleRepository articleRepository;
    private final CompetitionRepository competitionRepository;

    @Transactional(readOnly = true)
    public ProgressNewsDto news(UUID userId) {
        List<XpEvent> events = xpService.unseen(userId);
        var progress = xpService.summary(userId);
        int gained = events.stream().mapToInt(XpEvent::getAmount).sum();
        Levels.Level before = Levels.of(progress.getXp() - gained);

        Map<UUID, String> articleTitles = articleRepository.findAllById(refIds(events, XpSource.LESSON, XpSource.QUIZ_PERFECT))
                .stream().collect(Collectors.toMap(Article::getId, Article::getTitle));
        Map<UUID, Integer> rounds = competitionRepository.findAllById(refIds(events, XpSource.PORTFOLIO, XpSource.BEAT_CDI))
                .stream().collect(Collectors.toMap(Competition::getId, Competition::getRoundNumber));
        Set<String> achievementCodes = events.stream()
                .filter(e -> e.getSource() == XpSource.ACHIEVEMENT).map(XpEvent::getRefId).collect(Collectors.toSet());
        List<AchievementResponseDto> achievements = achievementService.listForUser(userId).stream()
                .filter(a -> achievementCodes.contains(a.getCode())).toList();
        Map<String, String> achievementTitles = achievements.stream()
                .collect(Collectors.toMap(AchievementResponseDto::getCode, AchievementResponseDto::getTitle));

        List<ProgressNewsDto.Item> items = events.stream()
                .map(e -> ProgressNewsDto.Item.builder()
                        .id(e.getId())
                        .source(e.getSource().name())
                        .label(label(e, articleTitles, rounds, achievementTitles))
                        .amount(e.getAmount())
                        .build())
                .toList();

        return ProgressNewsDto.builder()
                .items(items)
                .xpGained(gained)
                .levelUp(progress.getLevel() > before.number())
                .level(progress.getLevel())
                .levelTitle(progress.getLevelTitle())
                .achievements(achievements)
                .build();
    }

    @Transactional
    public void acknowledge(UUID userId, Collection<UUID> ids) {
        xpService.markSeen(userId, ids);
    }

    private static List<UUID> refIds(List<XpEvent> events, XpSource... sources) {
        Set<XpSource> wanted = Set.of(sources);
        return events.stream().filter(e -> wanted.contains(e.getSource()))
                .map(e -> UUID.fromString(e.getRefId())).distinct().toList();
    }

    private static String label(XpEvent e, Map<UUID, String> articles, Map<UUID, Integer> rounds,
                                Map<String, String> achievements) {
        Function<String, String> article = ref -> articles.getOrDefault(UUID.fromString(ref), "aula");
        Function<String, String> round = ref -> {
            Integer number = rounds.get(UUID.fromString(ref));
            return number == null ? "rodada" : "rodada " + number;
        };
        return switch (e.getSource()) {
            case LESSON -> "Aula concluída: " + article.apply(e.getRefId());
            case QUIZ_PERFECT -> "Nota máxima no quiz: " + article.apply(e.getRefId());
            case PORTFOLIO -> "Carteira enviada na " + round.apply(e.getRefId());
            case BEAT_CDI -> "Acima do CDI na " + round.apply(e.getRefId());
            case ACHIEVEMENT -> "Conquista: " + achievements.getOrDefault(e.getRefId(), e.getRefId());
        };
    }
}
