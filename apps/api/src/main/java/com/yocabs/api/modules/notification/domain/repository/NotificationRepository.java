package com.yocabs.api.modules.notification.domain.repository;

import com.yocabs.api.modules.notification.domain.model.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    List<Notification> findByRecipient(UUID recipientUserId, boolean unreadOnly, int limit);

    long countUnread(UUID recipientUserId);

    /** Marks one of the recipient's notifications read; returns false if it is not theirs. */
    boolean markRead(UUID notificationId, UUID recipientUserId);

    int markAllRead(UUID recipientUserId);
}
