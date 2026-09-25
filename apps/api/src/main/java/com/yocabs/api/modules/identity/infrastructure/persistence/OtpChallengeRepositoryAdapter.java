package com.yocabs.api.modules.identity.infrastructure.persistence;

import com.yocabs.api.modules.identity.domain.model.OtpChallenge;
import com.yocabs.api.modules.identity.domain.repository.OtpChallengeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class OtpChallengeRepositoryAdapter implements OtpChallengeRepository {

    private final OtpChallengeJpaRepository jpa;

    public OtpChallengeRepositoryAdapter(OtpChallengeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public OtpChallenge save(OtpChallenge challenge) {
        return jpa.save(OtpChallengeEntity.fromDomain(challenge)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OtpChallenge> findLatestByMobile(String mobile) {
        return jpa.findFirstByMobileOrderByCreatedAtDesc(mobile).map(OtpChallengeEntity::toDomain);
    }
}
