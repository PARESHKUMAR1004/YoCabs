package com.yocabs.api.shared.events;

import java.util.UUID;

/**
 * Published by business modules when someone should be told something.
 * Exactly one target is normally set: a single user, or every user of a
 * travel-partner organisation.
 */
public record NotificationRequested(
        UUID recipientUserId,
        UUID recipientPartnerId,
        String type,
        String title,
        String body,
        String referenceType,
        UUID referenceId
) {

    public static NotificationRequested toUser(
            UUID userId,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId
    ) {
        return new NotificationRequested(userId, null, type, title, body, referenceType, referenceId);
    }

    public static NotificationRequested toPartner(
            UUID partnerId,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId
    ) {
        return new NotificationRequested(null, partnerId, type, title, body, referenceType, referenceId);
    }
}
