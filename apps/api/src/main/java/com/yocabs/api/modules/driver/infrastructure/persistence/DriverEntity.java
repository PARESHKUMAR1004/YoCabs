package com.yocabs.api.modules.driver.infrastructure.persistence;

import com.yocabs.api.modules.driver.domain.model.Driver;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drivers")
public class DriverEntity {

    @Id
    private UUID id;

    @Column(name = "travel_partner_id", nullable = false)
    private UUID travelPartnerId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Driver.Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DriverEntity() {
        // JPA
    }

    static DriverEntity fromDomain(Driver driver) {
        DriverEntity entity = new DriverEntity();
        entity.id = driver.getId();
        entity.travelPartnerId = driver.getTravelPartnerId();
        entity.mobile = driver.getMobile();
        entity.createdAt = driver.getCreatedAt();
        entity.updateFromDomain(driver);
        return entity;
    }

    void updateFromDomain(Driver driver) {
        this.name = driver.getName();
        this.licenseNumber = driver.getLicenseNumber();
        this.status = driver.getStatus();
        this.updatedAt = driver.getUpdatedAt();
    }

    Driver toDomain() {
        return Driver.reconstitute(
                id, travelPartnerId, name, mobile, licenseNumber, status, createdAt, updatedAt
        );
    }
}
