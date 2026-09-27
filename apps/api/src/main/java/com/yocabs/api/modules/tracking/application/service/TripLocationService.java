package com.yocabs.api.modules.tracking.application.service;

import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.tracking.domain.model.TripLocation;
import com.yocabs.api.modules.tracking.domain.repository.TripLocationRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Live driver positions during a trip.
 *
 * Location is personal data, so it is fenced in three ways: a driver may only report against their
 * own trip, positions are only accepted and served while that trip is running or is due today with
 * a driver assigned, and
 * only the traveller on it, the partner running it, or an administrator may read one. Nothing is kept beyond the
 * current position, and a sweep clears anything a finished trip left behind.
 */
@Service
public class TripLocationService {

    private final TripLocationRepository locations;

    private final BookingRepository bookings;

    private final BookingService bookingService;

    private final TripRequestRepository tripRequests;

    private final TripEstimateService estimates;

    private final ZoneId zone;

    private final Duration staleAfter;

    public TripLocationService(
            TripLocationRepository locations,
            BookingRepository bookings,
            BookingService bookingService,
            TripRequestRepository tripRequests,
            TripEstimateService estimates,
            @Value("${yocabs.booking.zone:Asia/Kolkata}") String zone,
            @Value("${yocabs.tracking.stale-after-minutes:180}") long staleAfterMinutes
    ) {
        this.locations = locations;
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.tripRequests = tripRequests;
        this.estimates = estimates;
        this.zone = ZoneId.of(zone);
        this.staleAfter = Duration.ofMinutes(staleAfterMinutes);
    }

    /** The driver on the trip reports where they are. */
    @Transactional
    public TripLocation report(
            Actor actor,
            UUID bookingId,
            double latitude,
            double longitude,
            Double accuracyMetres,
            Double speedKph
    ) {
        actor.requireRole(Role.DRIVER);

        Booking booking = requireBooking(bookingId);

        if (!actor.userId().equals(booking.getDriverId())) {
            throw new AccessDeniedException("Not your assigned booking");
        }

        if (!isTrackable(booking)) {
            throw new IllegalStateException(
                    "Location is shared from the day of the trip, once a driver is assigned"
            );
        }

        return locations.save(
                new TripLocation(
                        bookingId,
                        actor.userId(),
                        latitude,
                        longitude,
                        accuracyMetres,
                        speedKph,
                        Instant.now()
                )
        );
    }

    /**
     * Where the trip starts, stops and ends, so a map can draw the journey the car is on. Anyone
     * who may see the booking may see this: it is what they already booked.
     */
    @Transactional(readOnly = true)
    public Itinerary route(Actor actor, UUID bookingId) {
        Booking booking = bookingService.get(actor, bookingId);

        return tripRequests
                .findById(new TripRequestId(booking.getTripRequestId()))
                .map(TripRequest::getItinerary)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + bookingId));
    }

    /** The car's position with how far and how long it still has to go, for the trip's viewers. */
    @Transactional(readOnly = true)
    public Optional<Tracked> track(Actor actor, UUID bookingId) {
        Booking booking = requireBooking(bookingId);

        requireWatcher(actor, booking);

        if (!isTrackable(booking)) {
            return Optional.empty();
        }

        // Before the trip starts the car is on its way to the pickup; after, to the destination.
        String phase = booking.getStatus() == BookingStatus.IN_PROGRESS ? "TO_DESTINATION" : "TO_PICKUP";

        return locations
                .findByBookingId(bookingId)
                .map(location ->
                        new Tracked(location, estimates.estimate(booking, location).orElse(null), phase)
                );
    }

    /**
     * The car may be followed while the trip runs, and from the day of the trip once a driver has
     * been assigned: that is when the traveller wants to know the driver is coming. Not before:
     * a driver is not tracked for a trip that is days away.
     */
    private boolean isTrackable(Booking booking) {
        if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            return true;
        }

        return booking.getStatus() == BookingStatus.CONFIRMED
                && booking.getDriverId() != null
                && !booking.getStartDate().isAfter(LocalDate.now(zone));
    }

    /** Every trip a partner currently has on the road, with its last known position. */
    @Transactional(readOnly = true)
    public List<LiveTrip> liveTripsForPartner(
            Actor actor,
            UUID travelPartnerId
    ) {
        actor.requirePartnerAccess(travelPartnerId);

        return toLiveTrips(
                bookings.findByPartnerId(
                        travelPartnerId,
                        BookingStatus.IN_PROGRESS
                )
        );
    }

    /** Every trip on the road across the marketplace. */
    @Transactional(readOnly = true)
    public List<LiveTrip> liveTripsForAdmin(
            Actor actor,
            int limit
    ) {
        actor.requireAdmin();

        return toLiveTrips(
                bookings.findAll(BookingStatus.IN_PROGRESS, limit)
        );
    }

    /**
     * Clears positions a finished trip left behind. Reads are already fenced to trips in progress,
     * so this is about not holding the data rather than about who can see it.
     */
    @Scheduled(fixedDelayString = "${yocabs.tracking.sweep-ms:900000}")
    @Transactional
    public void forgetStaleLocations() {

        Instant cutoff = Instant.now().minus(staleAfter);

        locations.deleteRecordedBefore(cutoff);
        estimates.forgetOlderThan(cutoff);
    }

    private List<LiveTrip> toLiveTrips(List<Booking> inProgress) {

        Map<UUID, TripLocation> byBooking =
                locations
                        .findByBookingIdIn(
                                inProgress.stream()
                                        .map(Booking::getId)
                                        .toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                TripLocation::bookingId,
                                Function.identity()
                        ));

        return inProgress.stream()
                .map(booking ->
                        new LiveTrip(
                                booking,
                                Optional.ofNullable(
                                        byBooking.get(booking.getId())
                                )
                        )
                )
                .toList();
    }

    private Booking requireBooking(UUID bookingId) {
        return bookings
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found: " + bookingId
                        )
                );
    }

    /** The partner running the trip, administrators, and the traveller on it may watch the car. */
    private void requireWatcher(Actor actor, Booking booking) {

        if (actor.isAdmin()) {
            return;
        }

        if (actor.role() == Role.TOURIST && actor.userId().equals(booking.getTouristId())) {
            return;
        }

        if (actor.isPartnerUser()) {
            actor.requirePartnerAccess(booking.getTravelPartnerId());
            return;
        }

        throw new AccessDeniedException(
                "Only the traveller, the travel partner and administrators can follow a trip"
        );
    }

    public record Tracked(
            TripLocation location,
            TripEstimateService.Estimate estimate,
            String phase
    ) {
    }

    public record LiveTrip(
            Booking booking,
            Optional<TripLocation> location
    ) {
    }
}
