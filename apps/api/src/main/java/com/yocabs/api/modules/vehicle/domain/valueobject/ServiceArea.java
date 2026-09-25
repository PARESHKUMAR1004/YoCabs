package com.yocabs.api.modules.vehicle.domain.valueobject;

import java.util.UUID;

public record ServiceArea(
        UUID id,
        String name,
        double latitude,
        double longitude,
        double radiusKm
) {

    public ServiceArea {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Service area ID is required"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Service area name is required"
            );
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90"
            );
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180"
            );
        }

        if (radiusKm <= 0) {
            throw new IllegalArgumentException(
                    "Service area radius must be greater than zero"
            );
        }

        name = name.trim();
    }

    public static ServiceArea create(
            String name,
            double latitude,
            double longitude,
            double radiusKm
    ) {
        return new ServiceArea(
                UUID.randomUUID(),
                name,
                latitude,
                longitude,
                radiusKm
        );
    }
}