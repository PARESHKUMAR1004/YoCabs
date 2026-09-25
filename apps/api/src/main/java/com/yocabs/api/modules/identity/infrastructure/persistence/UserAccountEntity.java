package com.yocabs.api.modules.identity.infrastructure.persistence;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.model.UserStatus;
import com.yocabs.api.shared.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {

    @Id
    private UUID id;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "preferred_language", length = 5)
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccountEntity() {
        // JPA
    }

    static UserAccountEntity fromDomain(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.id = account.getId();
        entity.mobile = account.getMobile();
        entity.email = account.getEmail();
        entity.passwordHash = account.getPasswordHash();
        entity.role = account.getRole();
        entity.partnerId = account.getPartnerId();
        entity.displayName = account.getDisplayName();
        entity.preferredLanguage = account.getPreferredLanguage();
        entity.status = account.getStatus();
        entity.failedLoginAttempts = account.getFailedLoginAttempts();
        entity.lockedUntil = account.getLockedUntil();
        entity.createdAt = account.getCreatedAt();
        entity.updatedAt = account.getUpdatedAt();
        return entity;
    }

    UserAccount toDomain() {
        return UserAccount.reconstitute(
                id, mobile, email, passwordHash, role, partnerId, displayName,
                preferredLanguage, status, failedLoginAttempts, lockedUntil, createdAt, updatedAt
        );
    }
}
