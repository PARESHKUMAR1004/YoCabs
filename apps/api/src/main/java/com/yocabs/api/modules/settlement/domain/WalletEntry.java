package com.yocabs.api.modules.settlement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable, append-only ledger line for a travel partner. */
public record WalletEntry(
        UUID id,
        UUID travelPartnerId,
        Type type,
        BigDecimal amount,
        String referenceType,
        UUID referenceId,
        String description,
        Instant createdAt
) {

    public enum Type {
        CREDIT,
        DEBIT
    }

    public WalletEntry {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Ledger amount must be greater than zero");
        }
    }

    public static WalletEntry of(
            UUID travelPartnerId,
            Type type,
            BigDecimal amount,
            String referenceType,
            UUID referenceId,
            String description
    ) {
        return new WalletEntry(
                UUID.randomUUID(), travelPartnerId, type, amount,
                referenceType, referenceId, description, Instant.now()
        );
    }

    public BigDecimal signedAmount() {
        return type == Type.CREDIT ? amount : amount.negate();
    }
}
