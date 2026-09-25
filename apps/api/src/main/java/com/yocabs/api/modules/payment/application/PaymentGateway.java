package com.yocabs.api.modules.payment.application;

import java.math.BigDecimal;

/** Port for the payment provider (Razorpay/PhonePe/... adapters implement this). */
public interface PaymentGateway {

    String name();

    /** Creates a provider-side order the client will pay against. */
    String createOrder(String receipt, BigDecimal amount, String currency);

    /** Refunds a captured payment; must be idempotent for the same idempotencyKey. */
    String refund(String gatewayPaymentId, BigDecimal amount, String currency, String idempotencyKey);

    boolean verifyWebhookSignature(String payload, String signature);

    WebhookEvent parseWebhook(String payload);

    record WebhookEvent(
            String eventId,
            Type type,
            String orderId,
            String paymentId,
            BigDecimal amount
    ) {

        public enum Type {
            PAYMENT_SUCCEEDED,
            PAYMENT_FAILED
        }
    }
}
