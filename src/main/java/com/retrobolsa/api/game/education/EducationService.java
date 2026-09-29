package com.retrobolsa.api.game.education;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.dto.ArticleResponseDto;
import com.retrobolsa.api.game.mission.MissionEvent;
import com.retrobolsa.api.game.mission.MissionService;
import com.retrobolsa.api.game.progress.ProgressService;
import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.game.progress.XpSource;
import com.retrobolsa.api.game.quiz.QuizQuestionRepository;
import com.retrobolsa.api.game.quiz.UserQuizAttemptRepository;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducationService {
    private final ArticleRepository articleRepository;
    private final UserArticleProgressRepository progressRepository;
    private final AchievementService achievementService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserQuizAttemptRepository quizAttemptRepository;
    private final ProgressService progressService;
    private final MissionService missionService;

    /**
     * Quatro queries no total, qualquer que seja o número de aulas: artigos com módulo
     * (fetch join), progresso do usuário, tamanho de cada quiz e melhores notas do usuário.
     */
    @Transactional(readOnly = true)
    public List<ArticleResponseDto> list(UUID userId) {
        Set<UUID> completedIds = progressRepository.findAllByIdUserId(userId).stream()
                .map(progress -> progress.getId().getArticleId())
                .collect(Collectors.toSet());
        Map<UUID, Long> quizSizes = quizQuestionRepository.countByArticle().stream()
                .collect(Collectors.toMap(QuizQuestionRepository.QuestionCount::getArticleId,
                        QuizQuestionRepository.QuestionCount::getTotal));
        Map<UUID, Integer> bestScores = quizAttemptRepository.findBestScores(userId).stream()
                .collect(Collectors.toMap(UserQuizAttemptRepository.BestScore::getArticleId,
                        UserQuizAttemptRepository.BestScore::getBestScore));
        return articleRepository.findAllByOrderByModule_DisplayOrderAscDisplayOrderAsc().stream()
                .map(article -> toDto(article, completedIds.contains(article.getId()),
                        quizSizes.getOrDefault(article.getId(), 0L).intValue(), bestScores.get(article.getId())))
                .toList();
    }

    /**
     * O botão "Marcar como concluída". Aula com quiz só se conclui passando nele, para
     * "concluída" significar a mesma coisa para todo mundo (e para o professor, mais adiante).
     */
    @Transactional
    public void completeWithoutQuiz(User user, UUID articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new IllegalArgumentException("Artigo nao encontrado");
        }
        if (quizQuestionRepository.existsByArticleId(articleId)) {
            throw new IllegalArgumentException("Esta aula se conclui pelo quiz");
        }
        complete(user, articleId);
    }

    /** Registra a conclusão (idempotente) e avalia as conquistas de aula. O QuizService chama ao aprovar. */
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
        progressService.reward(user, XpSource.LESSON, articleId.toString(), XpService.LESSON_XP);
        missionService.record(user, MissionEvent.LESSON, articleId.toString());
        UUID moduleId = article.getModule().getId();
        achievementService.evaluateOnLessonCompleted(user,
                progressRepository.countCompletedInModule(user.getId(), moduleId),
                articleRepository.countByModuleId(moduleId),
                progressRepository.countByIdUserId(user.getId()),
                articleRepository.count());
    }

    private ArticleResponseDto toDto(Article article, boolean completed, int quizTotal, Integer bestQuizScore) {
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
                .hasQuiz(quizTotal > 0)
                .quizTotal(quizTotal)
                .bestQuizScore(bestQuizScore)
                .build();
    }
}
