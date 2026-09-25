package com.yocabs.api.modules.identity.domain.repository;

import com.yocabs.api.modules.identity.domain.model.OtpChallenge;

import java.util.Optional;

public interface OtpChallengeRepository {

    OtpChallenge save(OtpChallenge challenge);

    Optional<OtpChallenge> findLatestByMobile(String mobile);
}
