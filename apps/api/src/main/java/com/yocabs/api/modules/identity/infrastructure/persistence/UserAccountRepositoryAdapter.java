package com.yocabs.api.modules.identity.infrastructure.persistence;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.shared.security.Role;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserAccountRepositoryAdapter implements UserAccountRepository {

    private final UserAccountJpaRepository jpa;

    public UserAccountRepositoryAdapter(UserAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount account) {
        return jpa.save(UserAccountEntity.fromDomain(account)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UUID id) {
        return jpa.findById(id).map(UserAccountEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByMobile(String mobile) {
        return jpa.findByMobile(mobile).map(UserAccountEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserAccountEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> findByRole(Role role) {
        return jpa.findByRole(role).stream().map(UserAccountEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> findByPartnerId(UUID partnerId) {
        return jpa.findByPartnerId(partnerId).stream().map(UserAccountEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return jpa.countByRole(role);
    }
}
