package com.yocabs.api.modules.popularplace.domain;

import java.time.Instant;
import java.util.UUID;

/** A destination an admin has curated as a quick pick on the tourist home screen. */
public record PopularPlace(
        UUID id,
        String name,
        String subtitle,
        double latitude,
        double longitude,
        int displayOrder,
        UUID updatedBy,
        Instant updatedAt
) {
    public PopularPlace {
        if (id == null) {
            throw new IllegalArgumentException("Id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }
}
