package com.retrobolsa.api.game.education;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_article_progress")
public class UserArticleProgress {
    @EmbeddedId
    private UserArticleProgressId id;
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}
