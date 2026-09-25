package com.yocabs.api.modules.audit.infrastructure;

import com.yocabs.api.modules.audit.domain.AuditLog;
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
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 30, updatable = false)
    private Role actorRole;

    @Column(name = "action", nullable = false, length = 80, updatable = false)
    private String action;

    @Column(name = "target_type", length = 40, updatable = false)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(name = "details", length = 2000, updatable = false)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
        // JPA
    }

    static AuditLogEntity fromDomain(AuditLog log) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = log.id();
        entity.actorId = log.actorId();
        entity.actorRole = log.actorRole();
        entity.action = log.action();
        entity.targetType = log.targetType();
        entity.targetId = log.targetId();
        entity.details = log.details();
        entity.createdAt = log.createdAt();
        return entity;
    }

    AuditLog toDomain() {
        return new AuditLog(id, actorId, actorRole, action, targetType, targetId, details, createdAt);
    }
}
