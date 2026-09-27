package com.yocabs.api.modules.notification.infrastructure;

import com.yocabs.api.modules.notification.application.DeviceTokenRepository;
import com.yocabs.api.modules.notification.application.PushChannels;
import com.yocabs.api.modules.notification.application.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Delivers notifications to phones through Expo's push service, which hands them on to Android's
 * Firebase Cloud Messaging. Delivery happens off the request thread: nobody waits on a phone.
 */
@Component
@Primary
@ConditionalOnProperty(name = "yocabs.push.expo.enabled", havingValue = "true", matchIfMissing = true)
public class ExpoPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushSender.class);

    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE =
            new ParameterizedTypeReference<>() { };

    private final RestClient http;
    private final DeviceTokenRepository devices;
    private final String url;
    private final String accessToken;
    private final Executor executor;

    @Autowired
    public ExpoPushSender(
            RestClient.Builder builder,
            DeviceTokenRepository devices,
            @Value("${yocabs.push.expo.url:https://exp.host/--/api/v2/push/send}") String url,
            @Value("${yocabs.push.expo.access-token:}") String accessToken
    ) {
        this(builder, devices, url, accessToken, Executors.newVirtualThreadPerTaskExecutor());
    }

    ExpoPushSender(
            RestClient.Builder builder,
            DeviceTokenRepository devices,
            String url,
            String accessToken,
            Executor executor
    ) {
        this.http = builder.build();
        this.devices = devices;
        this.url = url;
        this.accessToken = accessToken;
        this.executor = executor;
    }

    @Override
    public void send(PushMessage message) {
        List<String> tokens = devices.findTokens(message.recipientUserId());

        if (!tokens.isEmpty()) {
            executor.execute(() -> deliver(tokens, message));
        }
    }

    private void deliver(List<String> tokens, PushMessage message) {

        List<Map<String, Object>> payload = new ArrayList<>();

        for (String token : tokens) {
            payload.add(Map.of(
                    "to", token,
                    "title", message.title(),
                    "body", message.body(),
                    "channelId", PushChannels.forType(message.type()),
                    "sound", "default",
                    "priority", "high",
                    "data", data(message)
            ));
        }

        try {
            RestClient.RequestBodySpec request = http.post()
                    .uri(url)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");

            if (!accessToken.isBlank()) {
                request.header("Authorization", "Bearer " + accessToken);
            }

            Map<String, Object> response = request.body(payload).retrieve().body(RESPONSE);

            dropDeadTokens(tokens, response);

        } catch (RestClientException exception) {
            log.warn("Expo push delivery failed for user {}", message.recipientUserId(), exception);
        }
    }

    /** Expo answers one ticket per message, in order; a dead token is forgotten so it is not retried. */
    private void dropDeadTokens(List<String> tokens, Map<String, Object> response) {
        if (response == null || !(response.get("data") instanceof List<?> tickets)) {
            return;
        }

        for (int i = 0; i < tickets.size() && i < tokens.size(); i++) {
            if (tickets.get(i) instanceof Map<?, ?> ticket && "error".equals(ticket.get("status"))) {
                Object details = ticket.get("details");

                if (details instanceof Map<?, ?> map && "DeviceNotRegistered".equals(map.get("error"))) {
                    devices.discard(tokens.get(i));
                } else {
                    log.warn("Expo rejected a push: {}", ticket.get("message"));
                }
            }
        }
    }

    private static Map<String, Object> data(PushMessage message) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("type", message.type());
        if (message.referenceType() != null) {
            data.put("referenceType", message.referenceType());
        }
        if (message.referenceId() != null) {
            data.put("referenceId", message.referenceId().toString());
        }
        return data;
    }
}
