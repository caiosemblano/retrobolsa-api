package com.retrobolsa.api.user;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

    @Column
    private String username;

    @Column
    private String email;

    @Column
    private String passwordHash;

    @Column
    @Builder.Default
    private int totalScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private String role = "PLAYER";

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();


}



