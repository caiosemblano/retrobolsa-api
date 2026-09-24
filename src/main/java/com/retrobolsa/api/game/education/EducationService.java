package com.retrobolsa.api.game.education;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.dto.ArticleResponseDto;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducationService {
    private final ArticleRepository articleRepository;
    private final UserArticleProgressRepository progressRepository;
    private final AchievementService achievementService;

    /** Duas queries no total: artigos com módulo (fetch join) e todo o progresso do usuário. */
    @Transactional(readOnly = true)
    public List<ArticleResponseDto> list(UUID userId) {
        Set<UUID> completedIds = progressRepository.findAllByIdUserId(userId).stream()
                .map(progress -> progress.getId().getArticleId())
                .collect(Collectors.toSet());
        return articleRepository.findAllByOrderByModule_DisplayOrderAscDisplayOrderAsc().stream()
                .map(article -> toDto(article, completedIds.contains(article.getId())))
                .toList();
    }

    @Transactional
    public void complete(User user, UUID articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Artigo nao encontrado"));
        UserArticleProgressId progressId = new UserArticleProgressId(user.getId(), articleId);
        if (!progressRepository.existsById(progressId)) {
            UserArticleProgress progress = new UserArticleProgress();
            progress.setId(progressId);
            progress.setCompletedAt(LocalDateTime.now());
            progressRepository.save(progress);
        }

        // Avalia mesmo se a aula já estava concluída: o desbloqueio é idempotente, e assim
        // quem concluiu aulas antes das conquistas existirem as recebe na próxima conclusão.
        UUID moduleId = article.getModule().getId();
        achievementService.evaluateOnLessonCompleted(user,
                progressRepository.countCompletedInModule(user.getId(), moduleId),
                articleRepository.countByModuleId(moduleId),
                progressRepository.countByIdUserId(user.getId()),
                articleRepository.count());
    }

    private ArticleResponseDto toDto(Article article, boolean completed) {
        Module module = article.getModule();
        return ArticleResponseDto.builder()
                .id(article.getId())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .moduleDescription(module.getDescription())
                .moduleIcon(module.getIcon())
                .title(article.getTitle())
                .content(article.getContent())
                .durationMin(article.getDurationMin())
                .displayOrder(article.getDisplayOrder())
                .videoId(article.getVideoId())
                .completed(completed)
                .build();
    }
}
