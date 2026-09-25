package com.yocabs.api.modules.support.domain;

import com.yocabs.api.shared.security.Role;

import java.time.Instant;
import java.util.UUID;

/** Immutable message on a ticket. */
public record SupportMessage(
        UUID id,
        UUID ticketId,
        UUID authorId,
        Role authorRole,
        String body,
        Instant createdAt
) {

    public SupportMessage {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("A message cannot be empty");
        }
        if (body.length() > 4000) {
            throw new IllegalArgumentException("A message cannot exceed 4000 characters");
        }
        body = body.trim();
    }

    public static SupportMessage of(UUID ticketId, UUID authorId, Role authorRole, String body) {
        return new SupportMessage(UUID.randomUUID(), ticketId, authorId, authorRole, body, Instant.now());
    }
}
