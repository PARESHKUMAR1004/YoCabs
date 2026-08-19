package com.yocabs.api.modules.triprequest.domain.valueobject;

// Location.java
public record Location(
        String description,
        Double latitude,
        Double longitude
) {
    public Location {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Location description cannot be empty"
            );
        }

        description = description.trim();

        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "Latitude and longitude must both be provided or both be absent"
            );
        }

        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }
}