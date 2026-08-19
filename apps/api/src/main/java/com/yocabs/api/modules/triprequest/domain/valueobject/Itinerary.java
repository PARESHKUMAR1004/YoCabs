package com.yocabs.api.modules.triprequest.domain.valueobject;

import java.util.List;

public record Itinerary(
        Location pickup,
        List<Location> stops,
        Location destination
) {
    public Itinerary {
        if (pickup == null) {
            throw new IllegalArgumentException("Pickup location is required");
        }

        if (destination == null) {
            throw new IllegalArgumentException("Destination is required");
        }

        stops = stops == null ? List.of() : List.copyOf(stops);
    }
}