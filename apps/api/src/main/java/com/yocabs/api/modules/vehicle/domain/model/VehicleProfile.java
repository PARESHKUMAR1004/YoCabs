package com.yocabs.api.modules.vehicle.domain.model;

import java.time.Instant;
import java.time.Year;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Descriptive details and advertised facilities of a vehicle (separate from its fleet status). */
public class VehicleProfile {

    private final UUID vehicleId;
    private FuelType fuelType;
    private Transmission transmission;
    private Integer modelYear;
    private Integer luggageCapacity;
    private final Set<String> facilityCodes;
    private Instant updatedAt;

    private VehicleProfile(
            UUID vehicleId,
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity,
            Set<String> facilityCodes,
            Instant updatedAt
    ) {
        this.vehicleId = vehicleId;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.modelYear = modelYear;
        this.luggageCapacity = luggageCapacity;
        this.facilityCodes = new TreeSet<>(facilityCodes);
        this.updatedAt = updatedAt;
    }

    public static VehicleProfile empty(UUID vehicleId) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle is required");
        }
        return new VehicleProfile(vehicleId, null, null, null, null, Set.of(), Instant.now());
    }

    public static VehicleProfile reconstitute(
            UUID vehicleId,
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity,
            Set<String> facilityCodes,
            Instant updatedAt
    ) {
        return new VehicleProfile(
                vehicleId, fuelType, transmission, modelYear, luggageCapacity, facilityCodes, updatedAt
        );
    }

    public void updateDetails(
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity
    ) {
        if (modelYear != null && (modelYear < 1990 || modelYear > Year.now().getValue() + 1)) {
            throw new IllegalArgumentException("Model year is out of range");
        }

        if (luggageCapacity != null && (luggageCapacity < 0 || luggageCapacity > 50)) {
            throw new IllegalArgumentException("Luggage capacity must be between 0 and 50 bags");
        }

        this.fuelType = fuelType;
        this.transmission = transmission;
        this.modelYear = modelYear;
        this.luggageCapacity = luggageCapacity;
        this.updatedAt = Instant.now();
    }

    public void replaceFacilities(Set<String> codes) {
        facilityCodes.clear();
        facilityCodes.addAll(codes);
        updatedAt = Instant.now();
    }

    public UUID getVehicleId() { return vehicleId; }
    public FuelType getFuelType() { return fuelType; }
    public Transmission getTransmission() { return transmission; }
    public Integer getModelYear() { return modelYear; }
    public Integer getLuggageCapacity() { return luggageCapacity; }
    public Set<String> getFacilityCodes() { return Set.copyOf(facilityCodes); }
    public Instant getUpdatedAt() { return updatedAt; }
}
