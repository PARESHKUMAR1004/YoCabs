package com.yocabs.api.modules.audit.domain;

import com.yocabs.api.shared.security.Role;

import java.time.Instant;
import java.util.UUID;

/** Immutable audit record of a privileged action. */
public record AuditLog(
        UUID id,
        UUID actorId,
        Role actorRole,
        String action,
        String targetType,
        UUID targetId,
        String details,
        Instant createdAt
) {
}
