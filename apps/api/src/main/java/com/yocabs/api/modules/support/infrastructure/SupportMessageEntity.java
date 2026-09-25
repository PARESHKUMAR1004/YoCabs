package com.yocabs.api.modules.support.infrastructure;

import com.yocabs.api.modules.support.domain.SupportMessage;
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
@Table(name = "support_messages")
public class SupportMessageEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 30, updatable = false)
    private Role authorRole;

    @Column(name = "body", nullable = false, length = 4000, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SupportMessageEntity() {
        // JPA
    }

    static SupportMessageEntity fromDomain(SupportMessage message) {
        SupportMessageEntity entity = new SupportMessageEntity();
        entity.id = message.id();
        entity.ticketId = message.ticketId();
        entity.authorId = message.authorId();
        entity.authorRole = message.authorRole();
        entity.body = message.body();
        entity.createdAt = message.createdAt();
        return entity;
    }

    SupportMessage toDomain() {
        return new SupportMessage(id, ticketId, authorId, authorRole, body, createdAt);
    }
}
