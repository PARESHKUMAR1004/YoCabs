package com.yocabs.api.modules.tracking.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * The driver's latest known position on a trip. Only the current point is kept: the partner and
 * admin need to know where the car is now, not where it has been.
 */
public record TripLocation(
        UUID bookingId,
        UUID driverId,
        double latitude,
        double longitude,
        Double accuracyMetres,
        Double speedKph,
        Instant recordedAt
) {

    public TripLocation {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID is required");
        }

        if (driverId == null) {
            throw new IllegalArgumentException("Driver ID is required");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        if (recordedAt == null) {
            throw new IllegalArgumentException("Recorded timestamp is required");
        }
    }
}
