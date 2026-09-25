package com.yocabs.api.modules.identity.domain.repository;

import com.yocabs.api.modules.identity.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllForUser(UUID userId, Instant now);
}
