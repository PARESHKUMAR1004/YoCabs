package com.yocabs.api.modules.tracking.application.service;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.tracking.domain.model.TripLocation;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How far the car still has to go, and for how long. Worked out with the same routing provider
 * that priced the trip. The travellers' apps ask every few seconds, so an answer is reused for a
 * short while: a paid routing API is called at most once per trip per interval, not per poll.
 */
@Service
public class TripEstimateService {

    private static final Logger log = LoggerFactory.getLogger(TripEstimateService.class);

    private final RouteCalculationService routes;

    private final TripRequestRepository tripRequests;

    private final Duration reuseFor;

    private final Map<UUID, Remaining> remaining = new ConcurrentHashMap<>();

    private final Map<UUID, Double> tripDistances = new ConcurrentHashMap<>();

    public TripEstimateService(
            RouteCalculationService routes,
            TripRequestRepository tripRequests,
            @Value("${yocabs.tracking.estimate-reuse-seconds:20}") long reuseSeconds
    ) {
        this.routes = routes;
        this.tripRequests = tripRequests;
        this.reuseFor = Duration.ofSeconds(reuseSeconds);
    }

    /** Distance and time left from the car's position to the destination; empty when unknown. */
    public Optional<Estimate> estimate(Booking booking, TripLocation location) {

        Optional<Itinerary> itinerary = itineraryOf(booking);

        if (itinerary.isEmpty() || !hasCoordinates(itinerary.get().destination())) {
            return Optional.empty();
        }

        Remaining cached = remaining.get(booking.getId());

        if (cached != null && Instant.now().isBefore(cached.at().plus(reuseFor))) {
            return Optional.of(cached.estimate());
        }

        try {
            RouteCalculation left = routes.calculate(
                    new Itinerary(
                            new Location("Current position", location.latitude(), location.longitude()),
                            List.of(),
                            itinerary.get().destination()
                    )
            );

            Estimate estimate = new Estimate(
                    left.distanceKm().doubleValue(),
                    (int) Math.max(1, Math.ceil(left.duration().toSeconds() / 60.0)),
                    tripDistance(booking, itinerary.get())
            );

            remaining.put(booking.getId(), new Remaining(estimate, Instant.now()));

            return Optional.of(estimate);

        } catch (RuntimeException exception) {
            // A routing hiccup must not take live tracking down: fall back to the last answer.
            log.warn("Could not estimate the remaining trip for booking {}", booking.getId(), exception);
            return Optional.ofNullable(cached).map(Remaining::estimate);
        }
    }

    /** Forgets answers for trips that are no longer being followed. */
    public void forgetOlderThan(Instant cutoff) {
        remaining.entrySet().removeIf(entry -> entry.getValue().at().isBefore(cutoff));
        tripDistances.keySet().retainAll(remaining.keySet());
    }

    private Double tripDistance(Booking booking, Itinerary itinerary) {
        return tripDistances.computeIfAbsent(booking.getId(), id -> {
            if (!hasCoordinates(itinerary.pickup())) {
                return null;
            }
            try {
                return routes.calculate(itinerary).distanceKm().doubleValue();
            } catch (RuntimeException exception) {
                return null;
            }
        });
    }

    private Optional<Itinerary> itineraryOf(Booking booking) {
        return tripRequests
                .findById(new TripRequestId(booking.getTripRequestId()))
                .map(TripRequest::getItinerary);
    }

    private static boolean hasCoordinates(Location location) {
        return location.latitude() != null && location.longitude() != null;
    }

    private record Remaining(Estimate estimate, Instant at) {
    }

    /** What is left of the trip. {@code tripKm} is the whole journey, so an app can draw progress. */
    public record Estimate(double remainingKm, int remainingMinutes, Double tripKm) {
    }
}
