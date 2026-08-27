package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class VehicleEntity {

    @Id
    private UUID id;

    @Column(
            name = "travel_partner_id",
            nullable = false
    )
    private UUID travelPartnerId;

    @Column(
            name = "registration_number",
            nullable = false,
            length = 30
    )
    private String registrationNumber;

    @Column(
            name = "make",
            nullable = false,
            length = 100
    )
    private String make;

    @Column(
            name = "model",
            nullable = false,
            length = 100
    )
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            length = 30
    )
    private VehicleCategory category;

    @Column(
            name = "passenger_capacity",
            nullable = false
    )
    private int passengerCapacity;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private VehicleStatus status;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected VehicleEntity() {
        // JPA
    }

    private VehicleEntity(
            UUID id,
            UUID travelPartnerId,
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity,
            VehicleStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.travelPartnerId = travelPartnerId;
        this.registrationNumber = registrationNumber;
        this.make = make;
        this.model = model;
        this.category = category;
        this.passengerCapacity = passengerCapacity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static VehicleEntity fromDomain(
            Vehicle vehicle
    ) {
        return new VehicleEntity(
                vehicle.getId(),
                vehicle.getTravelPartnerId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getCategory(),
                vehicle.getPassengerCapacity(),
                vehicle.getStatus(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    public void updateFromDomain(
            Vehicle vehicle
    ) {
        this.registrationNumber =
                vehicle.getRegistrationNumber();

        this.make =
                vehicle.getMake();

        this.model =
                vehicle.getModel();

        this.category =
                vehicle.getCategory();

        this.passengerCapacity =
                vehicle.getPassengerCapacity();

        this.status =
                vehicle.getStatus();

        this.updatedAt =
                vehicle.getUpdatedAt();
    }

    public Vehicle toDomain() {

        return Vehicle.reconstitute(
                id,
                travelPartnerId,
                registrationNumber,
                make,
                model,
                category,
                passengerCapacity,
                status,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTravelPartnerId() {
        return travelPartnerId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }


}