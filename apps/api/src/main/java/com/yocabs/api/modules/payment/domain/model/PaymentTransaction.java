package com.yocabs.api.modules.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable ledger entry; rows are only ever appended. */
public record PaymentTransaction(
        UUID id,
        UUID paymentId,
        Type type,
        BigDecimal amount,
        Outcome status,
        String gatewayEventId,
        String gatewayReference,
        Instant createdAt
) {

    public enum Type {
        CHARGE,
        REFUND
    }

    public enum Outcome {
        SUCCEEDED,
        FAILED
    }

    public static PaymentTransaction record(
            UUID paymentId,
            Type type,
            BigDecimal amount,
            Outcome status,
            String gatewayEventId,
            String gatewayReference
    ) {
        return new PaymentTransaction(
                UUID.randomUUID(), paymentId, type, amount, status,
                gatewayEventId, gatewayReference, Instant.now()
        );
    }
}
