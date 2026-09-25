package com.yocabs.api.modules.identity.infrastructure.persistence;

import com.yocabs.api.shared.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountJpaRepository
        extends JpaRepository<UserAccountEntity, UUID> {

    Optional<UserAccountEntity> findByMobile(String mobile);

    Optional<UserAccountEntity> findByEmail(String email);

    List<UserAccountEntity> findByRole(Role role);

    List<UserAccountEntity> findByPartnerId(UUID partnerId);

    long countByRole(Role role);
}
