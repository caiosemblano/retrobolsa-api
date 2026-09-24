package com.retrobolsa.api.game.education;

import com.retrobolsa.api.game.achievement.AchievementService;
import com.retrobolsa.api.game.dto.ArticleResponseDto;
import com.retrobolsa.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EducationService — Listagem e Conclusão de Aulas")
class EducationServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private UserArticleProgressRepository progressRepository;
    @Mock private AchievementService achievementService;

    @InjectMocks private EducationService educationService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private final User ana = User.builder().id(UUID.randomUUID()).username("ana").role("PLAYER").build();

    private Module modulo(String titulo) {
        Module module = new Module();
        module.setId(UUID.randomUUID());
        module.setTitle(titulo);
        module.setDescription("Descrição de " + titulo);
        module.setIcon("calculator");
        module.setDisplayOrder(1);
        return module;
    }

    private Article aula(Module module, String titulo, int ordem, String videoId) {
        Article article = new Article();
        article.setId(UUID.randomUUID());
        article.setModule(module);
        article.setTitle(titulo);
        article.setContent("Conteúdo de " + titulo);
        article.setDurationMin(5);
        article.setDisplayOrder(ordem);
        article.setVideoId(videoId);
        return article;
    }

    private UserArticleProgress progresso(User user, Article article) {
        UserArticleProgress progress = new UserArticleProgress();
        progress.setId(new UserArticleProgressId(user.getId(), article.getId()));
        progress.setCompletedAt(LocalDateTime.now());
        return progress;
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("list")
    class Listagem {

        @Test
        @DisplayName("marca as concluídas a partir de uma única busca de progresso e expõe módulo e vídeo")
        void listaComProgressoEDadosDoModulo() {
            Module matematica = modulo("Matemática Financeira");
            Article rentabilidade = aula(matematica, "O que é rentabilidade?", 1, "Y9ng5fVji-A");
            Article juros = aula(matematica, "Juros simples vs. compostos", 2, "G6LJcuKY80c");
            when(articleRepository.findAllByOrderByModule_DisplayOrderAscDisplayOrderAsc())
                    .thenReturn(List.of(rentabilidade, juros));
            when(progressRepository.findAllByIdUserId(ana.getId())).thenReturn(List.of(progresso(ana, juros)));

            List<ArticleResponseDto> lista = educationService.list(ana.getId());

            assertThat(lista).extracting(ArticleResponseDto::isCompleted).containsExactly(false, true);
            ArticleResponseDto primeira = lista.get(0);
            assertThat(primeira.getVideoId()).isEqualTo("Y9ng5fVji-A");
            assertThat(primeira.getModuleTitle()).isEqualTo("Matemática Financeira");
            assertThat(primeira.getModuleDescription()).isEqualTo("Descrição de Matemática Financeira");
            assertThat(primeira.getModuleIcon()).isEqualTo("calculator");
            assertThat(primeira.getDisplayOrder()).isEqualTo(1);
            // Nada de consulta por artigo: era o N+1 da versão anterior.
            verify(progressRepository, never()).existsByIdUserIdAndIdArticleId(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // complete
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("complete")
    class Conclusao {

        @Test
        @DisplayName("grava o progresso e avalia as conquistas com as contagens do módulo e do total")
        void gravaEAvalia() {
            Module matematica = modulo("Matemática Financeira");
            Article juros = aula(matematica, "Juros simples vs. compostos", 2, "G6LJcuKY80c");
            when(articleRepository.findById(juros.getId())).thenReturn(Optional.of(juros));
            when(progressRepository.existsById(any())).thenReturn(false);
            when(progressRepository.countCompletedInModule(ana.getId(), matematica.getId())).thenReturn(3L);
            when(articleRepository.countByModuleId(matematica.getId())).thenReturn(3L);
            when(progressRepository.countByIdUserId(ana.getId())).thenReturn(5L);
            when(articleRepository.count()).thenReturn(8L);

            educationService.complete(ana, juros.getId());

            verify(progressRepository).save(any(UserArticleProgress.class));
            verify(achievementService).evaluateOnLessonCompleted(ana, 3L, 3L, 5L, 8L);
        }

        @Test
        @DisplayName("aula já concluída não grava de novo, mas ainda avalia (para quem concluiu antes das conquistas existirem)")
        void jaConcluidaAindaAvalia() {
            Module matematica = modulo("Matemática Financeira");
            Article juros = aula(matematica, "Juros simples vs. compostos", 2, "G6LJcuKY80c");
            when(articleRepository.findById(juros.getId())).thenReturn(Optional.of(juros));
            when(progressRepository.existsById(any())).thenReturn(true);

            educationService.complete(ana, juros.getId());

            verify(progressRepository, never()).save(any());
            verify(achievementService).evaluateOnLessonCompleted(any(), anyLong(), anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("aula inexistente é recusada sem gravar nem avaliar nada")
        void aulaInexistente() {
            UUID inexistente = UUID.randomUUID();
            when(articleRepository.findById(inexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> educationService.complete(ana, inexistente))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Artigo nao encontrado");
            verify(progressRepository, never()).save(any());
            verifyNoInteractions(achievementService);
        }
    }
}
