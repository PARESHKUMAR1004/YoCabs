package com.yocabs.api.modules.settlement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payout {

    public enum Status {
        REQUESTED,
        PAID,
        REJECTED
    }

    private final UUID id;
    private final UUID travelPartnerId;
    private final BigDecimal amount;
    private Status status;
    private final UUID requestedBy;
    private UUID processedBy;
    private String bankReference;
    private String note;
    private final Instant createdAt;
    private Instant processedAt;

    private Payout(
            UUID id,
            UUID travelPartnerId,
            BigDecimal amount,
            Status status,
            UUID requestedBy,
            UUID processedBy,
            String bankReference,
            String note,
            Instant createdAt,
            Instant processedAt
    ) {
        this.id = id;
        this.travelPartnerId = travelPartnerId;
        this.amount = amount;
        this.status = status;
        this.requestedBy = requestedBy;
        this.processedBy = processedBy;
        this.bankReference = bankReference;
        this.note = note;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static Payout request(UUID travelPartnerId, BigDecimal amount, UUID requestedBy) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payout amount must be greater than zero");
        }
        return new Payout(
                UUID.randomUUID(), travelPartnerId, amount, Status.REQUESTED,
                requestedBy, null, null, null, Instant.now(), null
        );
    }

    public static Payout reconstitute(
            UUID id,
            UUID travelPartnerId,
            BigDecimal amount,
            Status status,
            UUID requestedBy,
            UUID processedBy,
            String bankReference,
            String note,
            Instant createdAt,
            Instant processedAt
    ) {
        return new Payout(
                id, travelPartnerId, amount, status, requestedBy, processedBy,
                bankReference, note, createdAt, processedAt
        );
    }

    public void markPaid(UUID adminId, String bankReference) {
        requireRequested();
        if (bankReference == null || bankReference.isBlank()) {
            throw new IllegalArgumentException("A bank reference is required to mark a payout paid");
        }
        this.status = Status.PAID;
        this.processedBy = adminId;
        this.bankReference = bankReference.trim();
        this.processedAt = Instant.now();
    }

    public void reject(UUID adminId, String note) {
        requireRequested();
        this.status = Status.REJECTED;
        this.processedBy = adminId;
        this.note = note;
        this.processedAt = Instant.now();
    }

    private void requireRequested() {
        if (status != Status.REQUESTED) {
            throw new IllegalStateException("This payout has already been processed");
        }
    }

    public UUID getId() { return id; }
    public UUID getTravelPartnerId() { return travelPartnerId; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public UUID getRequestedBy() { return requestedBy; }
    public UUID getProcessedBy() { return processedBy; }
    public String getBankReference() { return bankReference; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
