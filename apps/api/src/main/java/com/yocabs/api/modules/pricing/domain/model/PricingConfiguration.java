package com.yocabs.api.modules.pricing.domain.model;

import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.time.Instant;
import java.util.UUID;

public abstract class PricingConfiguration {

    private final UUID id;

    private final UUID vehicleId;

    private final TripType tripType;

    private boolean active;

    private final Instant createdAt;

    private Instant updatedAt;

    protected PricingConfiguration(
            UUID id,
            UUID vehicleId,
            TripType tripType
    ) {
        this(
                id,
                vehicleId,
                tripType,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    protected PricingConfiguration(
            UUID id,
            UUID vehicleId,
            TripType tripType,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Pricing configuration ID is required"
            );
        }

        if (vehicleId == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (tripType == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
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

        this.id = id;
        this.vehicleId = vehicleId;
        this.tripType = tripType;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public TripType getTripType() {
        return tripType;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void activate() {
        active = true;
        markUpdated();
    }

    public void deactivate() {
        active = false;
        markUpdated();
    }

    protected void markUpdated() {
        updatedAt = Instant.now();
    }
}