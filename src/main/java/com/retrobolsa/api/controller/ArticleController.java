package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.ArticleResponseDto;
import com.retrobolsa.api.game.dto.QuizQuestionDto;
import com.retrobolsa.api.game.dto.QuizResultDto;
import com.retrobolsa.api.game.dto.SubmitQuizRequestDto;
import com.retrobolsa.api.game.education.EducationService;
import com.retrobolsa.api.game.quiz.QuizService;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {
    private final EducationService educationService;
    private final QuizService quizService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ArticleResponseDto>> list(Authentication authentication) {
        return ResponseEntity.ok(educationService.list(resolveUser(authentication).getId()));
    }

    /** Para aulas sem quiz; as que têm quiz se concluem pelo POST /{id}/quiz. */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable UUID id, Authentication authentication) {
        educationService.completeWithoutQuiz(resolveUser(authentication), id);
        return ResponseEntity.noContent().build();
    }

    /** Perguntas do quiz da aula, sem as respostas; lista vazia se a aula não tem quiz. */
    @GetMapping("/{id}/quiz")
    public ResponseEntity<List<QuizQuestionDto>> quiz(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.getQuiz(id));
    }

    @PostMapping("/{id}/quiz")
    public ResponseEntity<QuizResultDto> submitQuiz(@PathVariable UUID id,
                                                    @Valid @RequestBody SubmitQuizRequestDto request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(quizService.submit(resolveUser(authentication), id, request));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }
}
