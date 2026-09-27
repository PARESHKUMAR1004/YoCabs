package com.yocabs.api.modules.notification.application;

import java.util.UUID;

/** Port for out-of-app delivery (FCM/APNs/SMS/WhatsApp adapters implement this). */
public interface PushSender {

    void send(PushMessage message);

    /** What to tell a user's phone. The type decides how it sounds; the reference decides where a tap goes. */
    record PushMessage(
            UUID recipientUserId,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId
    ) {
    }
}
