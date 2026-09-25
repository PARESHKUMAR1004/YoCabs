package com.yocabs.api.modules.payment.infrastructure;

import com.yocabs.api.modules.payment.application.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Deterministic, offline stand-in for a real provider. It uses the same
 * HMAC-signed webhook contract a real gateway would, so the verification and
 * idempotency logic is genuinely exercised. Never allowed in production.
 */
@Component
@ConditionalOnProperty(name = "yocabs.payment.gateway", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentGateway implements PaymentGateway {

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public SandboxPaymentGateway(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${yocabs.payment.webhook-secret:sandbox-webhook-secret-change-me}") String secret
    ) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "The sandbox payment gateway must not be used in production; "
                            + "configure a real gateway adapter (yocabs.payment.gateway)"
            );
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String name() {
        return "sandbox";
    }

    @Override
    public String createOrder(String receipt, BigDecimal amount, String currency) {
        return "order_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String refund(String gatewayPaymentId, BigDecimal amount, String currency, String idempotencyKey) {
        return "rfnd_" + UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign webhook payload", exception);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || signature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public WebhookEvent parseWebhook(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            String type = json.path("type").asString("");

            return new WebhookEvent(
                    json.path("eventId").asString(""),
                    switch (type) {
                        case "PAYMENT_SUCCEEDED" -> WebhookEvent.Type.PAYMENT_SUCCEEDED;
                        case "PAYMENT_FAILED" -> WebhookEvent.Type.PAYMENT_FAILED;
                        default -> throw new IllegalArgumentException("Unsupported webhook event type");
                    },
                    json.path("orderId").asString(""),
                    json.path("paymentId").asString(""),
                    new BigDecimal(json.path("amount").asString("0"))
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Malformed webhook payload");
        }
    }
}
