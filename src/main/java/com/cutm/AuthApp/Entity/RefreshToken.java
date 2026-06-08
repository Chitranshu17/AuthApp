package com.cutm.AuthApp.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                // Instantly speeds up queries when you search for a token during the /refresh endpoint
                @Index(name = "idx_token", columnList = "jti", unique = true),
                // Speeds up queries when you want to delete all tokens for a specific user
                @Index(name = "idx_user_id", columnList = "user_id")
        }
)
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The actual 7-day refresh token string (or the JTI if you are tracking the JWT ID)
    @Column(nullable = false, unique = true, updatable = false)
    private String jti;

    // The exact moment this token dies.
    @Column(nullable = false)
    private OffsetDateTime expiryDate;

    @Column(nullable = false)
    private OffsetDateTime createdDate;

    // The "Kill Switch": Set this to true if the user logs out, instantly killing the session.
    @Builder.Default // Tells Lombok to use this default value when building
    @Column(nullable = false)
    private boolean revoked = false;

    // The Relationship: Every token belongs to one specific User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String replacedByToken;
}