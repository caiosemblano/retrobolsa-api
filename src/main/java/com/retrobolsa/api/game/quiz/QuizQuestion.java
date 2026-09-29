package com.retrobolsa.api.game.quiz;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pergunta de múltipla escolha de uma aula. Os IDs vêm do seed (família ffffffff-). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    private UUID id;

    /** Só o ID, como no progresso das aulas: o quiz não precisa carregar o artigo. */
    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private String prompt;

    /** Por que a resposta certa é a certa; mostrada logo depois de responder. */
    @Column(nullable = false)
    private String explanation;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<QuizOption> options = new ArrayList<>();
}
