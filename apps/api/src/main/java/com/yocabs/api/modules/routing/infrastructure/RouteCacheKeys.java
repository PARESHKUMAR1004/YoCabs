package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A cache key for a route that two searches for the same physical trip will share, even when the
 * free-text place descriptions differ ("BBSR Airport" vs "Bhubaneswar Airport"). Coordinates are
 * rounded to five decimal places (about a metre), which is far tighter than routing itself cares
 * about, so this never merges two genuinely different pickups.
 */
public final class RouteCacheKeys {

    private RouteCacheKeys() {
    }

    public static String of(Itinerary itinerary) {
        return Stream.concat(
                        Stream.of(itinerary.pickup()),
                        Stream.concat(itinerary.stops().stream(), Stream.of(itinerary.destination())))
                .map(RouteCacheKeys::coordinate)
                .collect(Collectors.joining(">"));
    }

    private static String coordinate(Location location) {
        return round(location.latitude()) + "," + round(location.longitude());
    }

    private static double round(double value) {
        return Math.round(value * 100_000) / 100_000.0;
    }
}
