package com.retrobolsa.api.game.achievement;

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
public class UserAchievementId implements Serializable {
    private UUID userId;
    private UUID achievementId;
}
