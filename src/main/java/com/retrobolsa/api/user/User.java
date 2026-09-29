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

    /** Já viu o passo a passo do primeiro acesso. */
    @Column(nullable = false)
    @Builder.Default
    private boolean onboarded = false;

    /** Quando marcou, no cadastro, que tem 18 anos ou autorização do responsável. */
    @Column(name = "consented_at")
    private LocalDateTime consentedAt;

    /** Entrou com uma senha temporária dada pelo admin: precisa trocá-la antes de seguir. */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;


}



