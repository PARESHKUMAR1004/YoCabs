package com.yocabs.api.modules.broadcast.domain.model;

import java.time.Instant;
import java.util.UUID;

public class BroadcastRecipient {

    private final UUID id;

    private final UUID broadcastId;

    private final UUID travelPartnerId;

    private BroadcastRecipientStatus status;

    private final Instant createdAt;

    private Instant updatedAt;

    private BroadcastRecipient(
            UUID id,
            UUID broadcastId,
            UUID travelPartnerId,
            BroadcastRecipientStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.broadcastId = broadcastId;
        this.travelPartnerId = travelPartnerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BroadcastRecipient create(
            UUID broadcastId,
            UUID travelPartnerId
    ) {

        if (broadcastId == null) {
            throw new IllegalArgumentException(
                    "Broadcast ID is required"
            );
        }

        if (travelPartnerId == null) {
            throw new IllegalArgumentException(
                    "Travel partner ID is required"
            );
        }

        Instant now = Instant.now();

        return new BroadcastRecipient(
                UUID.randomUUID(),
                broadcastId,
                travelPartnerId,
                BroadcastRecipientStatus.PENDING,
                now,
                now
        );
    }

    public void markSent() {

        if (status != BroadcastRecipientStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending recipient can be marked as sent"
            );
        }

        status = BroadcastRecipientStatus.SENT;
        updatedAt = Instant.now();
    }

    public void markViewed() {

        if (status != BroadcastRecipientStatus.SENT) {
            throw new IllegalStateException(
                    "Only a sent broadcast can be marked as viewed"
            );
        }

        status = BroadcastRecipientStatus.VIEWED;
        updatedAt = Instant.now();
    }

    public void markResponded() {

        if (status != BroadcastRecipientStatus.SENT
                && status != BroadcastRecipientStatus.VIEWED) {

            throw new IllegalStateException(
                    "Only a sent or viewed broadcast can receive a response"
            );
        }

        status = BroadcastRecipientStatus.RESPONDED;
        updatedAt = Instant.now();
    }

    public void decline() {

        if (status != BroadcastRecipientStatus.SENT
                && status != BroadcastRecipientStatus.VIEWED) {

            throw new IllegalStateException(
                    "Only a sent or viewed broadcast can be declined"
            );
        }

        status = BroadcastRecipientStatus.DECLINED;
        updatedAt = Instant.now();
    }

    public void expire() {

        if (status == BroadcastRecipientStatus.RESPONDED
                || status == BroadcastRecipientStatus.DECLINED
                || status == BroadcastRecipientStatus.EXPIRED) {

            throw new IllegalStateException(
                    "Recipient cannot be expired in its current state"
            );
        }

        status = BroadcastRecipientStatus.EXPIRED;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBroadcastId() {
        return broadcastId;
    }

    public UUID getTravelPartnerId() {
        return travelPartnerId;
    }

    public BroadcastRecipientStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}