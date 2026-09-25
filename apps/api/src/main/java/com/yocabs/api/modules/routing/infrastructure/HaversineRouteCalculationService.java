package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider-free estimator: great-circle distance between consecutive
 * points scaled by a road-winding factor. Used when no routing provider
 * is configured; the full multi-stop journey is always routed.
 */
@Component
@ConditionalOnProperty(
        name = "yocabs.routing.provider",
        havingValue = "haversine",
        matchIfMissing = true
)
public class HaversineRouteCalculationService
        implements RouteCalculationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final double roadFactor;
    private final double averageSpeedKmh;

    public HaversineRouteCalculationService(
            @Value("${yocabs.routing.haversine.road-factor:1.3}") double roadFactor,
            @Value("${yocabs.routing.haversine.average-speed-kmh:40}") double averageSpeedKmh
    ) {
        if (roadFactor < 1 || averageSpeedKmh <= 0) {
            throw new IllegalArgumentException(
                    "Invalid haversine routing configuration"
            );
        }
        this.roadFactor = roadFactor;
        this.averageSpeedKmh = averageSpeedKmh;
    }

    @Override
    public RouteCalculation calculate(Itinerary itinerary) {

        List<Location> points = RoutePoints.of(itinerary);

        double straightLineKm = 0;
        for (int i = 1; i < points.size(); i++) {
            straightLineKm += distanceKm(points.get(i - 1), points.get(i));
        }

        BigDecimal distance =
                BigDecimal.valueOf(straightLineKm * roadFactor)
                        .setScale(2, RoundingMode.HALF_UP);

        long seconds = Math.max(
                60,
                Math.round(distance.doubleValue() / averageSpeedKmh * 3600)
        );

        return new RouteCalculation(distance, Duration.ofSeconds(seconds));
    }

    private static double distanceKm(Location a, Location b) {

        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());

        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dLon / 2), 2);

        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }

    /** Ordered pickup -> stops -> destination points, all with coordinates. */
    static final class RoutePoints {

        private RoutePoints() {
        }

        static List<Location> of(Itinerary itinerary) {

            if (itinerary == null) {
                throw new IllegalArgumentException("Itinerary is required");
            }

            List<Location> points = new ArrayList<>();
            points.add(itinerary.pickup());
            points.addAll(itinerary.stops());
            points.add(itinerary.destination());

            for (Location point : points) {
                if (point.latitude() == null || point.longitude() == null) {
                    throw new IllegalArgumentException(
                            "Coordinates are required for routing: "
                                    + point.description()
                    );
                }
            }

            return points;
        }
    }
}
