package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.FuelType;
import com.yocabs.api.modules.vehicle.domain.model.Transmission;
import com.yocabs.api.modules.vehicle.domain.model.VehicleProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "vehicle_profiles")
public class VehicleProfileEntity {

    @Id
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission", length = 20)
    private Transmission transmission;

    @Column(name = "model_year")
    private Short modelYear;

    @Column(name = "luggage_capacity")
    private Short luggageCapacity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vehicle_facilities", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "facility_code", nullable = false, length = 40)
    private Set<String> facilityCodes = new HashSet<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VehicleProfileEntity() {
        // JPA
    }

    static VehicleProfileEntity fromDomain(VehicleProfile profile) {
        VehicleProfileEntity entity = new VehicleProfileEntity();
        entity.vehicleId = profile.getVehicleId();
        entity.apply(profile);
        return entity;
    }

    void apply(VehicleProfile profile) {
        this.fuelType = profile.getFuelType();
        this.transmission = profile.getTransmission();
        this.modelYear = profile.getModelYear() == null ? null : profile.getModelYear().shortValue();
        this.luggageCapacity =
                profile.getLuggageCapacity() == null ? null : profile.getLuggageCapacity().shortValue();
        this.facilityCodes.clear();
        this.facilityCodes.addAll(profile.getFacilityCodes());
        this.updatedAt = profile.getUpdatedAt();
    }

    VehicleProfile toDomain() {
        return VehicleProfile.reconstitute(
                vehicleId,
                fuelType,
                transmission,
                modelYear == null ? null : modelYear.intValue(),
                luggageCapacity == null ? null : luggageCapacity.intValue(),
                facilityCodes,
                updatedAt
        );
    }
}
