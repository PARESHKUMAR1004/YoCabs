package com.yocabs.api.modules.vehicle.domain.model;

import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Vehicle {

    private final UUID id;

    private final UUID travelPartnerId;

    private VehicleCategory category;

    private String registrationNumber;

    private String make;

    private String model;

    private int passengerCapacity;

    private VehicleStatus status;

    private final Instant createdAt;

    private Instant updatedAt;

    /**
     * Where this vehicle will travel from. Areas belong to the vehicle rather than the partner,
     * so one fleet can cover different ground with different cars. Loaded alongside the vehicle.
     */
    private List<ServiceArea> serviceAreas = List.of();

    private Vehicle(
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

    public static Vehicle create(
            UUID travelPartnerId,
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity
    ) {

        validateTravelPartnerId(travelPartnerId);
        validateRegistrationNumber(registrationNumber);
        validateMake(make);
        validateModel(model);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Vehicle category is required"
            );
        }

        validatePassengerCapacity(passengerCapacity);

        Instant now = Instant.now();

        return new Vehicle(
                UUID.randomUUID(),
                travelPartnerId,
                registrationNumber.trim().toUpperCase(),
                make.trim(),
                model.trim(),
                category,
                passengerCapacity,
                VehicleStatus.UNAVAILABLE,
                now,
                now
        );
    }

    public void makeAvailable() {

        if (status == VehicleStatus.INACTIVE) {
            throw new IllegalStateException(
                    "An inactive vehicle cannot be made available"
            );
        }

        if (status == VehicleStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "A vehicle under maintenance cannot be made available"
            );
        }

        status = VehicleStatus.AVAILABLE;
        updatedAt = Instant.now();
    }

    public void makeUnavailable() {

        if (status == VehicleStatus.INACTIVE) {
            throw new IllegalStateException(
                    "An inactive vehicle cannot be made unavailable"
            );
        }

        status = VehicleStatus.UNAVAILABLE;
        updatedAt = Instant.now();
    }

    public void sendToMaintenance() {

        if (status == VehicleStatus.INACTIVE) {
            throw new IllegalStateException(
                    "An inactive vehicle cannot be sent to maintenance"
            );
        }

        status = VehicleStatus.MAINTENANCE;
        updatedAt = Instant.now();
    }

    public void deactivate() {

        status = VehicleStatus.INACTIVE;
        updatedAt = Instant.now();
    }

    private static void validateTravelPartnerId(
            UUID travelPartnerId
    ) {
        if (travelPartnerId == null) {
            throw new IllegalArgumentException(
                    "Travel partner ID is required"
            );
        }
    }

    private static void validateRegistrationNumber(
            String registrationNumber
    ) {
        if (registrationNumber == null
                || registrationNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Vehicle registration number is required"
            );
        }
    }

    private static void validateMake(
            String make
    ) {
        if (make == null || make.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle make is required"
            );
        }
    }

    private static void validateModel(
            String model
    ) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle model is required"
            );
        }
    }

    public void updateDetails(
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity
    ) {

        if (registrationNumber == null
                || registrationNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle registration number is required"
            );
        }

        if (make == null || make.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle make is required"
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle model is required"
            );
        }

        if (category == null) {
            throw new IllegalArgumentException(
                    "Vehicle category is required"
            );
        }

        if (passengerCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Passenger capacity must be greater than zero"
            );
        }

        this.registrationNumber =
                registrationNumber.trim().toUpperCase();

        this.make =
                make.trim();

        this.model =
                model.trim();

        this.category =
                category;

        this.passengerCapacity =
                passengerCapacity;

        this.updatedAt =
                Instant.now();
    }

    private static void validatePassengerCapacity(
            int passengerCapacity
    ) {
        if (passengerCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Passenger capacity must be greater than zero"
            );
        }
    }

    public static Vehicle reconstitute(
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

        if (id == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (travelPartnerId == null) {
            throw new IllegalArgumentException(
                    "Travel partner ID is required"
            );
        }

        if (registrationNumber == null
                || registrationNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle registration number is required"
            );
        }

        if (make == null || make.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle make is required"
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle model is required"
            );
        }

        if (category == null) {
            throw new IllegalArgumentException(
                    "Vehicle category is required"
            );
        }

        if (passengerCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Passenger capacity must be greater than zero"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Vehicle status is required"
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

        return new Vehicle(
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

    public VehicleCategory getCategory() {
        return category;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
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

    public List<ServiceArea> getServiceAreas() {
        return serviceAreas;
    }

    /**
     * Attaches the areas loaded with this vehicle. The repository is the only caller: areas live
     * in their own table, so they arrive separately from the vehicle row.
     */
    public void attachServiceAreas(
            List<ServiceArea> areas
    ) {
        this.serviceAreas =
                areas == null
                        ? List.of()
                        : List.copyOf(areas);
    }

    /**
     * True when the pickup falls inside any of this vehicle's areas. A vehicle with no area
     * configured covers nothing, so it never appears in a search.
     */
    public boolean coversLocation(
            Double latitude,
            Double longitude
    ) {

        if (latitude == null || longitude == null) {
            return false;
        }

        return serviceAreas.stream()
                .anyMatch(area ->
                        distanceKm(
                                latitude,
                                longitude,
                                area.latitude(),
                                area.longitude()
                        ) <= area.radiusKm()
                );
    }

    /** Great-circle distance in kilometres. */
    private static double distanceKm(
            double fromLatitude,
            double fromLongitude,
            double toLatitude,
            double toLongitude
    ) {

        final double earthRadiusKm = 6371.0;

        double latitudeDelta =
                Math.toRadians(toLatitude - fromLatitude);

        double longitudeDelta =
                Math.toRadians(toLongitude - fromLongitude);

        double a =
                Math.sin(latitudeDelta / 2)
                        * Math.sin(latitudeDelta / 2)
                        + Math.cos(Math.toRadians(fromLatitude))
                        * Math.cos(Math.toRadians(toLatitude))
                        * Math.sin(longitudeDelta / 2)
                        * Math.sin(longitudeDelta / 2);

        return earthRadiusKm * 2
                * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );
    }
}
