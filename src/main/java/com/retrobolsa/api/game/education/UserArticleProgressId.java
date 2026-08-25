package com.retrobolsa.api.game.education;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserArticleProgressId implements Serializable {
    private UUID userId;
    private UUID articleId;
}
