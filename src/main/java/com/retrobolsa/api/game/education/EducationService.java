package com.retrobolsa.api.game.education;

import com.retrobolsa.api.game.dto.ArticleResponseDto;
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
    public void complete(UUID userId, UUID articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new IllegalArgumentException("Artigo nao encontrado");
        }
        UserArticleProgressId progressId = new UserArticleProgressId(userId, articleId);
        if (!progressRepository.existsById(progressId)) {
            UserArticleProgress progress = new UserArticleProgress();
            progress.setId(progressId);
            progress.setCompletedAt(LocalDateTime.now());
            progressRepository.save(progress);
        }
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
