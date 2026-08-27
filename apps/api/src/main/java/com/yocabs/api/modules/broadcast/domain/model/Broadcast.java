package com.yocabs.api.modules.broadcast.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Broadcast {

    private final UUID id;
    private final UUID tripRequestId;

    private BroadcastStatus status;

    private final Instant createdAt;
    private Instant updatedAt;

    private Broadcast(
            UUID id,
            UUID tripRequestId,
            BroadcastStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.tripRequestId = tripRequestId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Broadcast create(
            UUID tripRequestId
    ) {
        Instant now = Instant.now();

        return new Broadcast(
                UUID.randomUUID(),
                tripRequestId,
                BroadcastStatus.CREATED,
                now,
                now
        );
    }

    public void publish() {

        if (status != BroadcastStatus.CREATED) {
            throw new IllegalStateException(
                    "Only a newly created broadcast can be published"
            );
        }

        status = BroadcastStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void close() {

        if (status != BroadcastStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active broadcast can be closed"
            );
        }

        status = BroadcastStatus.CLOSED;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTripRequestId() {
        return tripRequestId;
    }

    public BroadcastStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}