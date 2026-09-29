package com.retrobolsa.api.game.quiz;

import com.retrobolsa.api.game.dto.QuizQuestionDto;
import com.retrobolsa.api.game.dto.QuizResultDto;
import com.retrobolsa.api.game.dto.SubmitQuizRequestDto;
import com.retrobolsa.api.game.education.ArticleRepository;
import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.education.EducationService;
import com.retrobolsa.api.game.progress.ProgressService;
import com.retrobolsa.api.game.progress.XpService;
import com.retrobolsa.api.game.progress.XpSource;
import com.retrobolsa.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final UserQuizAttemptRepository attemptRepository;
    private final ArticleRepository articleRepository;
    private final EducationService educationService;
    private final ProgressService progressService;
    private final AchievementService achievementService;

    /** Acertar pelo menos esta fração das perguntas conclui a aula (2 de 3). */
    static boolean passed(int score, int total) {
        return total > 0 && score * 3 >= total * 2;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDto> getQuiz(UUID articleId) {
        requireArticle(articleId);
        return questionRepository.findWithOptionsByArticleId(articleId).stream()
                .map(question -> QuizQuestionDto.builder()
                        .id(question.getId())
                        .prompt(question.getPrompt())
                        .options(question.getOptions().stream()
                                .map(option -> QuizQuestionDto.Option.builder()
                                        .id(option.getId()).text(option.getText()).build())
                                .toList())
                        .build())
                .toList();
    }

    /**
     * Corrige, guarda a tentativa com cada resposta e, se passou, conclui a aula
     * pelo mesmo caminho do botão (o que também avalia as conquistas de aula).
     */
    @Transactional
    public QuizResultDto submit(User user, UUID articleId, SubmitQuizRequestDto request) {
        requireArticle(articleId);
        List<QuizQuestion> questions = questionRepository.findWithOptionsByArticleId(articleId);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Esta aula nao tem quiz");
        }

        Map<UUID, UUID> chosen = new HashMap<>();
        for (SubmitQuizRequestDto.Answer answer : request.getAnswers()) {
            if (chosen.put(answer.getQuestionId(), answer.getOptionId()) != null) {
                throw new IllegalArgumentException("Pergunta respondida mais de uma vez");
            }
        }
        if (chosen.size() != questions.size()
                || !questions.stream().allMatch(q -> chosen.containsKey(q.getId()))) {
            throw new IllegalArgumentException("Responda todas as perguntas do quiz");
        }

        UserQuizAttempt attempt = new UserQuizAttempt();
        attempt.setUserId(user.getId());
        attempt.setArticleId(articleId);

        List<QuizResultDto.QuestionResult> results = new ArrayList<>();
        int score = 0;
        for (QuizQuestion question : questions) {
            UUID selected = chosen.get(question.getId());
            QuizOption option = question.getOptions().stream()
                    .filter(o -> o.getId().equals(selected))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Alternativa invalida para a pergunta"));
            UUID correctOption = question.getOptions().stream()
                    .filter(QuizOption::isCorrect)
                    .map(QuizOption::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Pergunta sem alternativa certa: " + question.getId()));
            if (option.isCorrect()) score++;

            UserQuizAnswer answer = new UserQuizAnswer();
            answer.setId(new UserQuizAnswer.Id(null, question.getId()));
            answer.setAttempt(attempt);
            answer.setOptionId(selected);
            answer.setCorrect(option.isCorrect());
            attempt.getAnswers().add(answer);

            results.add(QuizResultDto.QuestionResult.builder()
                    .questionId(question.getId())
                    .selectedOptionId(selected)
                    .correctOptionId(correctOption)
                    .correct(option.isCorrect())
                    .explanation(question.getExplanation())
                    .build());
        }

        attempt.setScore(score);
        attempt.setTotal(questions.size());
        attemptRepository.save(attempt);

        boolean passed = passed(score, questions.size());
        if (passed) {
            educationService.complete(user, articleId);
        }
        boolean perfect = score == questions.size();
        if (perfect) {
            progressService.reward(user, XpSource.QUIZ_PERFECT, articleId.toString(), XpService.QUIZ_PERFECT_XP);
        }
        achievementService.evaluateOnQuiz(user, perfect, attemptRepository.countPerfectQuizzes(user.getId()));
        return QuizResultDto.builder()
                .score(score)
                .total(questions.size())
                .passed(passed)
                .results(results)
                .build();
    }

    private void requireArticle(UUID articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new IllegalArgumentException("Artigo nao encontrado");
        }
    }
}
