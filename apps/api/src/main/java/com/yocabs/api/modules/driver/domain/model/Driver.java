package com.yocabs.api.modules.driver.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A partner's driver. Its id is the id of the driver's login account. */
public class Driver {

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    private final UUID id;
    private final UUID travelPartnerId;
    private String name;
    private final String mobile;
    private String licenseNumber;
    private Status status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Driver(
            UUID id,
            UUID travelPartnerId,
            String name,
            String mobile,
            String licenseNumber,
            Status status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.travelPartnerId = travelPartnerId;
        this.name = name;
        this.mobile = mobile;
        this.licenseNumber = licenseNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Driver create(
            UUID id,
            UUID travelPartnerId,
            String name,
            String mobile,
            String licenseNumber
    ) {
        if (id == null || travelPartnerId == null) {
            throw new IllegalArgumentException("Driver and travel partner are required");
        }
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("A valid driver name is required");
        }
        if (mobile == null || mobile.isBlank()) {
            throw new IllegalArgumentException("Driver mobile number is required");
        }
        if (licenseNumber == null || licenseNumber.isBlank() || licenseNumber.length() > 50) {
            throw new IllegalArgumentException("A valid driving licence number is required");
        }

        Instant now = Instant.now();

        return new Driver(
                id, travelPartnerId, name.trim(), mobile,
                licenseNumber.trim().toUpperCase(), Status.ACTIVE, now, now
        );
    }

    public static Driver reconstitute(
            UUID id,
            UUID travelPartnerId,
            String name,
            String mobile,
            String licenseNumber,
            Status status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Driver(id, travelPartnerId, name, mobile, licenseNumber, status, createdAt, updatedAt);
    }

    public void deactivate() {
        if (status == Status.INACTIVE) {
            throw new IllegalStateException("Driver is already inactive");
        }
        status = Status.INACTIVE;
        updatedAt = Instant.now();
    }

    public void activate() {
        if (status == Status.ACTIVE) {
            throw new IllegalStateException("Driver is already active");
        }
        status = Status.ACTIVE;
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public UUID getId() { return id; }
    public UUID getTravelPartnerId() { return travelPartnerId; }
    public String getName() { return name; }
    public String getMobile() { return mobile; }
    public String getLicenseNumber() { return licenseNumber; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
