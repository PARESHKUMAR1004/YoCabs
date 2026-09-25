package com.yocabs.api.modules.identity.domain.model;

import com.yocabs.api.shared.security.Role;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserAccount {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private final String mobile;
    private String email;
    private String passwordHash;
    private final Role role;
    private final UUID partnerId;
    private String displayName;
    private String preferredLanguage;
    private UserStatus status;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserAccount(
            UUID id,
            String mobile,
            String email,
            String passwordHash,
            Role role,
            UUID partnerId,
            String displayName,
            String preferredLanguage,
            UserStatus status,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.mobile = mobile;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.partnerId = partnerId;
        this.displayName = displayName;
        this.preferredLanguage = preferredLanguage;
        this.status = status;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserAccount newTourist(String mobile) {
        return mobileUser(mobile, Role.TOURIST, null, null);
    }

    public static UserAccount newPartnerUser(
            String mobile,
            String displayName,
            Role role,
            UUID partnerId
    ) {
        if (role != Role.PARTNER_OWNER && role != Role.PARTNER_STAFF) {
            throw new IllegalArgumentException("Not a partner role: " + role);
        }
        if (partnerId == null) {
            throw new IllegalArgumentException("Travel partner is required");
        }
        return mobileUser(mobile, role, partnerId, displayName);
    }

    public static UserAccount newDriver(String mobile, String displayName, UUID partnerId) {
        if (partnerId == null) {
            throw new IllegalArgumentException("Travel partner is required");
        }
        return mobileUser(mobile, Role.DRIVER, partnerId, displayName);
    }

    public static UserAccount newAdmin(
            String email,
            String passwordHash,
            String displayName,
            Role role
    ) {
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Not an admin role: " + role);
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        Instant now = Instant.now();
        return new UserAccount(
                UUID.randomUUID(), null, email.trim().toLowerCase(), passwordHash,
                role, null, displayName, null, UserStatus.ACTIVE, 0, null, now, now
        );
    }

    private static UserAccount mobileUser(
            String mobile,
            Role role,
            UUID partnerId,
            String displayName
    ) {
        if (mobile == null || mobile.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        Instant now = Instant.now();
        return new UserAccount(
                UUID.randomUUID(), mobile, null, null, role, partnerId,
                displayName, null, UserStatus.ACTIVE, 0, null, now, now
        );
    }

    public static UserAccount reconstitute(
            UUID id,
            String mobile,
            String email,
            String passwordHash,
            Role role,
            UUID partnerId,
            String displayName,
            String preferredLanguage,
            UserStatus status,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new UserAccount(
                id, mobile, email, passwordHash, role, partnerId, displayName,
                preferredLanguage, status, failedLoginAttempts, lockedUntil, createdAt, updatedAt
        );
    }

    public void updateProfile(String displayName, String email, String preferredLanguage) {

        if (displayName != null) {
            String name = displayName.trim();
            if (name.isEmpty() || name.length() > 200) {
                throw new IllegalArgumentException("Name must be 1-200 characters");
            }
            this.displayName = name;
        }

        if (email != null) {
            String normalized = email.trim().toLowerCase();

            if (!normalized.isEmpty()) {
                if (normalized.length() > 200 || !EMAIL.matcher(normalized).matches()) {
                    throw new IllegalArgumentException("Enter a valid email address");
                }
                if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
                    if (!normalized.equals(this.email)) {
                        throw new IllegalArgumentException("An administrator's login email cannot be changed here");
                    }
                }
                this.email = normalized;
            } else if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
                this.email = null;
            }
        }

        if (preferredLanguage != null) {
            String language = preferredLanguage.trim().toLowerCase();
            if (!language.matches("^[a-z]{2}$")) {
                throw new IllegalArgumentException("Language must be a two-letter code such as en, hi or or");
            }
            this.preferredLanguage = language;
        }

        this.updatedAt = Instant.now();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
        }
        updatedAt = now;
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public void block() {
        status = UserStatus.BLOCKED;
        updatedAt = Instant.now();
    }

    public void unblock() {
        status = UserStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UUID getPartnerId() { return partnerId; }
    public String getDisplayName() { return displayName; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
