package com.retrobolsa.api.game.education;

import com.retrobolsa.api.game.dto.ArticleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EducationService {
    private final ArticleRepository articleRepository;
    private final UserArticleProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public List<ArticleResponseDto> list(UUID userId) {
        return articleRepository.findAllByOrderByModule_DisplayOrderAscDisplayOrderAsc().stream()
                .map(article -> toDto(article, userId))
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

    private ArticleResponseDto toDto(Article article, UUID userId) {
        return ArticleResponseDto.builder()
                .id(article.getId())
                .moduleId(article.getModule().getId())
                .moduleTitle(article.getModule().getTitle())
                .title(article.getTitle())
                .content(article.getContent())
                .durationMin(article.getDurationMin())
                .completed(progressRepository.existsByIdUserIdAndIdArticleId(userId, article.getId()))
                .build();
    }
}
