package com.yocabs.api.modules.identity.infrastructure.persistence;

import com.yocabs.api.modules.identity.domain.model.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {
        // JPA
    }

    static RefreshTokenEntity fromDomain(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = token.getId();
        entity.userId = token.getUserId();
        entity.tokenHash = token.getTokenHash();
        entity.expiresAt = token.getExpiresAt();
        entity.revokedAt = token.getRevokedAt();
        entity.createdAt = token.getCreatedAt();
        return entity;
    }

    RefreshToken toDomain() {
        return RefreshToken.reconstitute(id, userId, tokenHash, expiresAt, revokedAt, createdAt);
    }
}
