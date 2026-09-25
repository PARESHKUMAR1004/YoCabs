package com.yocabs.api.modules.identity.domain.repository;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.shared.security.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByMobile(String mobile);

    Optional<UserAccount> findByEmail(String email);

    List<UserAccount> findByRole(Role role);

    List<UserAccount> findByPartnerId(UUID partnerId);

    long countByRole(Role role);
}
