package com.retrobolsa.api.game.education;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "articles")
public class Article {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "duration_min")
    private int durationMin;
    @Column(name = "display_order")
    private int displayOrder;
    /** ID do vídeo no YouTube (11 caracteres, validado por CHECK no banco); null se a aula não tem vídeo. */
    @Column(name = "video_id")
    private String videoId;
}
