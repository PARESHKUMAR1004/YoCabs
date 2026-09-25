package com.yocabs.api.modules.notification.infrastructure;

import com.yocabs.api.modules.notification.application.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Default no-provider sender; a real provider adapter should be marked @Primary. */
@Component
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(UUID recipientUserId, String title, String body) {
        log.debug("Push (not delivered, no provider configured) to {}: {}", recipientUserId, title);
    }
}
