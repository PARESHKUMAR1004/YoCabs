package com.yocabs.api.modules.notification.infrastructure.persistence;

import com.yocabs.api.modules.notification.domain.model.Notification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "type", nullable = false, length = 60)
    private String type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEntity() {
        // JPA
    }

    static NotificationEntity fromDomain(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.id = notification.getId();
        entity.recipientUserId = notification.getRecipientUserId();
        entity.type = notification.getType();
        entity.title = notification.getTitle();
        entity.body = notification.getBody();
        entity.referenceType = notification.getReferenceType();
        entity.referenceId = notification.getReferenceId();
        entity.readAt = notification.getReadAt();
        entity.createdAt = notification.getCreatedAt();
        return entity;
    }

    Notification toDomain() {
        return Notification.reconstitute(
                id, recipientUserId, type, title, body, referenceType, referenceId, readAt, createdAt
        );
    }
}
