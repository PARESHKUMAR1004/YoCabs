package com.yocabs.api.modules.travelpartner.domain.model;


import java.time.Instant;
import java.util.UUID;

public class TravelPartner {

    private final UUID id;

    private String name;

    private TravelPartnerStatus status;

    private final Instant createdAt;

    private Instant updatedAt;

    private TravelPartner(
            UUID id,
            String name,
            TravelPartnerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TravelPartner create(
            String name
    ) {
        validateName(name);

        Instant now = Instant.now();

        return new TravelPartner(
                UUID.randomUUID(),
                name.trim(),
                TravelPartnerStatus.PENDING_APPROVAL,
                now,
                now
        );
    }

    public void activate() {

        if (status != TravelPartnerStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only a partner pending approval can be activated"
            );
        }

        status = TravelPartnerStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void reinstate() {

        if (status != TravelPartnerStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Only a suspended partner can be reinstated"
            );
        }

        status = TravelPartnerStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void suspend() {

        if (status != TravelPartnerStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active partner can be suspended"
            );
        }

        status = TravelPartnerStatus.SUSPENDED;
        updatedAt = Instant.now();
    }

    public void deactivate() {

        if (status == TravelPartnerStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Travel partner is already inactive"
            );
        }

        status = TravelPartnerStatus.INACTIVE;
        updatedAt = Instant.now();
    }

    private static void validateName(
            String name
    ) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Travel partner name is required"
            );
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException(
                    "Travel partner name cannot exceed 200 characters"
            );
        }
    }

    public static TravelPartner reconstitute(
            UUID id,
            String name,
            TravelPartnerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Travel partner ID is required"
            );
        }

        validateName(name);

        if (status == null) {
            throw new IllegalArgumentException(
                    "Travel partner status is required"
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "Created timestamp is required"
            );
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException(
                    "Updated timestamp is required"
            );
        }

        return new TravelPartner(
                id,
                name,
                status,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TravelPartnerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }


}