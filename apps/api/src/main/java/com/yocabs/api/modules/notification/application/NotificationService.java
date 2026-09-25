package com.yocabs.api.modules.notification.application;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.notification.domain.model.Notification;
import com.yocabs.api.modules.notification.domain.repository.NotificationRepository;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final UserAccountRepository users;
    private final PushSender pushSender;

    public NotificationService(
            NotificationRepository notifications,
            UserAccountRepository users,
            PushSender pushSender
    ) {
        this.notifications = notifications;
        this.users = users;
        this.pushSender = pushSender;
    }

    /** Runs inside the publisher's transaction, so a notification exists iff the change committed. */
    @EventListener
    @Transactional
    public void on(NotificationRequested event) {

        Set<UUID> recipients = new LinkedHashSet<>();

        if (event.recipientUserId() != null) {
            users.findById(event.recipientUserId())
                    .filter(UserAccount::isActive)
                    .ifPresentOrElse(
                            user -> recipients.add(user.getId()),
                            () -> log.warn("Skipping notification for unknown/blocked user {}",
                                    event.recipientUserId())
                    );
        }

        if (event.recipientPartnerId() != null) {
            users.findByPartnerId(event.recipientPartnerId()).stream()
                    .filter(UserAccount::isActive)
                    .filter(user -> user.getRole() == Role.PARTNER_OWNER
                            || user.getRole() == Role.PARTNER_STAFF)
                    .forEach(user -> recipients.add(user.getId()));
        }

        for (UUID recipient : recipients) {
            notifications.save(
                    Notification.create(
                            recipient, event.type(), event.title(), event.body(),
                            event.referenceType(), event.referenceId()
                    )
            );

            try {
                pushSender.send(recipient, event.title(), event.body());
            } catch (RuntimeException exception) {
                log.warn("Push delivery failed for {}", recipient, exception);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> list(Actor actor, boolean unreadOnly, int limit) {
        return notifications.findByRecipient(actor.userId(), unreadOnly, limit);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Actor actor) {
        return notifications.countUnread(actor.userId());
    }

    @Transactional
    public void markRead(Actor actor, UUID notificationId) {
        if (!notifications.markRead(notificationId, actor.userId())) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
    }

    @Transactional
    public int markAllRead(Actor actor) {
        return notifications.markAllRead(actor.userId());
    }
}
