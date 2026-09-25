package com.yocabs.api.modules.notification.application;

import java.util.UUID;

/** Port for out-of-app delivery (FCM/APNs/SMS/WhatsApp adapters implement this). */
public interface PushSender {

    void send(UUID recipientUserId, String title, String body);
}
