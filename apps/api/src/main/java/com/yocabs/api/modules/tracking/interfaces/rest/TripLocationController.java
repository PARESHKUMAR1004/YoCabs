package com.yocabs.api.modules.tracking.interfaces.rest;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.tracking.application.service.TripLocationService;
import com.yocabs.api.modules.tracking.application.service.TripLocationService.LiveTrip;
import com.yocabs.api.modules.tracking.domain.model.TripLocation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class TripLocationController {

    private final TripLocationService tripLocationService;

    public TripLocationController(
            TripLocationService tripLocationService
    ) {
        this.tripLocationService = tripLocationService;
    }

    /** The driver's app posts this while the trip is under way. */
    @PostMapping("/api/v1/bookings/{bookingId}/location")
    public LocationResponse report(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId,
            @RequestBody LocationRequest request
    ) {
        return LocationResponse.from(
                tripLocationService.report(
                        actor,
                        bookingId,
                        request.latitude(),
                        request.longitude(),
                        request.accuracyMetres(),
                        request.speedKph()
                )
        );
    }

    /** Where the car is now. Empty until the driver starts the trip and shares a position. */
    @GetMapping("/api/v1/bookings/{bookingId}/location")
    public LocationResponse latest(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return tripLocationService
                .latest(actor, bookingId)
                .map(LocationResponse::from)
                .orElse(null);
    }

    /** The journey itself (pickup, stops, destination) for drawing on a map. */
    @GetMapping("/api/v1/bookings/{bookingId}/route")
    public RouteResponse route(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return RouteResponse.from(tripLocationService.route(actor, bookingId));
    }

    /** The partner's own trips currently on the road. */
    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/live-trips")
    public List<LiveTripResponse> forPartner(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId
    ) {
        return tripLocationService
                .liveTripsForPartner(actor, travelPartnerId)
                .stream()
                .map(LiveTripResponse::from)
                .toList();
    }

    /** Every trip on the road, for the operations console. */
    @GetMapping("/api/v1/admin/live-trips")
    public List<LiveTripResponse> forAdmin(
            @CurrentActor Actor actor,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return tripLocationService
                .liveTripsForAdmin(actor, limit)
                .stream()
                .map(LiveTripResponse::from)
                .toList();
    }

    public record LocationRequest(
            double latitude,
            double longitude,
            Double accuracyMetres,
            Double speedKph
    ) {
    }

    public record LocationResponse(
            UUID bookingId,
            double latitude,
            double longitude,
            Double accuracyMetres,
            Double speedKph,
            Instant recordedAt
    ) {
        static LocationResponse from(TripLocation location) {
            return new LocationResponse(
                    location.bookingId(),
                    location.latitude(),
                    location.longitude(),
                    location.accuracyMetres(),
                    location.speedKph(),
                    location.recordedAt()
            );
        }
    }

    public record PlaceResponse(String description, Double latitude, Double longitude) {
        static PlaceResponse from(Location location) {
            return new PlaceResponse(
                    location.description(), location.latitude(), location.longitude()
            );
        }
    }

    public record RouteResponse(
            PlaceResponse pickup,
            List<PlaceResponse> stops,
            PlaceResponse destination
    ) {
        static RouteResponse from(Itinerary itinerary) {
            return new RouteResponse(
                    PlaceResponse.from(itinerary.pickup()),
                    itinerary.stops().stream().map(PlaceResponse::from).toList(),
                    PlaceResponse.from(itinerary.destination())
            );
        }
    }

    public record LiveTripResponse(
            UUID bookingId,
            UUID travelPartnerId,
            UUID vehicleId,
            UUID driverId,
            String pickup,
            String destination,
            LocationResponse location
    ) {
        static LiveTripResponse from(LiveTrip trip) {
            Booking booking = trip.booking();

            return new LiveTripResponse(
                    booking.getId(),
                    booking.getTravelPartnerId(),
                    booking.getVehicleId(),
                    booking.getDriverId(),
                    booking.getPickupDescription(),
                    booking.getDestinationDescription(),
                    trip.location()
                            .map(LocationResponse::from)
                            .orElse(null)
            );
        }
    }
}
