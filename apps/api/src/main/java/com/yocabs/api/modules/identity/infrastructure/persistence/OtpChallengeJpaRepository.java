package com.yocabs.api.modules.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeJpaRepository
        extends JpaRepository<OtpChallengeEntity, UUID> {

    Optional<OtpChallengeEntity> findFirstByMobileOrderByCreatedAtDesc(String mobile);
}
