package com.yocabs.api.modules.identity.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class OtpChallenge {

    private final UUID id;
    private final String mobile;
    private final String codeHash;
    private int attempts;
    private final Instant expiresAt;
    private Instant consumedAt;
    private final Instant createdAt;

    private OtpChallenge(
            UUID id,
            String mobile,
            String codeHash,
            int attempts,
            Instant expiresAt,
            Instant consumedAt,
            Instant createdAt
    ) {
        this.id = id;
        this.mobile = mobile;
        this.codeHash = codeHash;
        this.attempts = attempts;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }

    public static OtpChallenge issue(String mobile, String codeHash, Duration validity) {
        Instant now = Instant.now();
        return new OtpChallenge(UUID.randomUUID(), mobile, codeHash, 0, now.plus(validity), null, now);
    }

    public static OtpChallenge reconstitute(
            UUID id,
            String mobile,
            String codeHash,
            int attempts,
            Instant expiresAt,
            Instant consumedAt,
            Instant createdAt
    ) {
        return new OtpChallenge(id, mobile, codeHash, attempts, expiresAt, consumedAt, createdAt);
    }

    public boolean isUsable(Instant now, int maxAttempts) {
        return consumedAt == null && expiresAt.isAfter(now) && attempts < maxAttempts;
    }

    public void registerAttempt() {
        attempts++;
    }

    public void consume(Instant now) {
        consumedAt = now;
    }

    public UUID getId() { return id; }
    public String getMobile() { return mobile; }
    public String getCodeHash() { return codeHash; }
    public int getAttempts() { return attempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
