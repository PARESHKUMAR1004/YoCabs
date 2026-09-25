package com.yocabs.api.modules.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final UUID id;
    private final UUID recipientUserId;
    private final String type;
    private final String title;
    private final String body;
    private final String referenceType;
    private final UUID referenceId;
    private Instant readAt;
    private final Instant createdAt;

    private Notification(
            UUID id,
            UUID recipientUserId,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId,
            Instant readAt,
            Instant createdAt
    ) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public static Notification create(
            UUID recipientUserId,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId
    ) {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient is required");
        }

        return new Notification(
                UUID.randomUUID(), recipientUserId, type,
                truncate(title, 200), truncate(body, 1000), referenceType, referenceId,
                null, Instant.now()
        );
    }

    public static Notification reconstitute(
            UUID id,
            UUID recipientUserId,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId,
            Instant readAt,
            Instant createdAt
    ) {
        return new Notification(
                id, recipientUserId, type, title, body, referenceType, referenceId, readAt, createdAt
        );
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    public UUID getId() { return id; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
