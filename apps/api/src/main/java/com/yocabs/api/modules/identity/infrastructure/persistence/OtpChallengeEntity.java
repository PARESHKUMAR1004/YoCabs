package com.yocabs.api.modules.identity.infrastructure.persistence;

import com.yocabs.api.modules.identity.domain.model.OtpChallenge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
public class OtpChallengeEntity {

    @Id
    private UUID id;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpChallengeEntity() {
        // JPA
    }

    static OtpChallengeEntity fromDomain(OtpChallenge challenge) {
        OtpChallengeEntity entity = new OtpChallengeEntity();
        entity.id = challenge.getId();
        entity.mobile = challenge.getMobile();
        entity.codeHash = challenge.getCodeHash();
        entity.attempts = challenge.getAttempts();
        entity.expiresAt = challenge.getExpiresAt();
        entity.consumedAt = challenge.getConsumedAt();
        entity.createdAt = challenge.getCreatedAt();
        return entity;
    }

    OtpChallenge toDomain() {
        return OtpChallenge.reconstitute(id, mobile, codeHash, attempts, expiresAt, consumedAt, createdAt);
    }
}
