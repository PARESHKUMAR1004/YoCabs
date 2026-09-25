package com.yocabs.api.modules.notification.infrastructure.persistence;

import com.yocabs.api.modules.notification.domain.model.Notification;
import com.yocabs.api.modules.notification.domain.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        return jpa.save(NotificationEntity.fromDomain(notification)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(UUID recipientUserId, boolean unreadOnly, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));

        return (unreadOnly
                ? jpa.findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(recipientUserId, page)
                : jpa.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, page))
                .stream().map(NotificationEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID recipientUserId) {
        return jpa.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
    }

    @Override
    @Transactional
    public boolean markRead(UUID notificationId, UUID recipientUserId) {
        if (!jpa.existsByIdAndRecipientUserId(notificationId, recipientUserId)) {
            return false;
        }
        jpa.markRead(notificationId, recipientUserId, Instant.now());
        return true;
    }

    @Override
    @Transactional
    public int markAllRead(UUID recipientUserId) {
        return jpa.markAllRead(recipientUserId, Instant.now());
    }
}
