package com.retrobolsa.api.game.achievement;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_achievements")
public class UserAchievement {
    @EmbeddedId
    private UserAchievementId id;
    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;
}
