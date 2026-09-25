package com.yocabs.api.modules.tracking.application.service;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.tracking.domain.model.TripLocation;
import com.yocabs.api.modules.tracking.domain.repository.TripLocationRepository;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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
 * own trip, positions are only accepted and served while that trip is actually in progress, and
 * only the partner running the trip or an administrator may read one. Nothing is kept beyond the
 * current position, and a sweep clears anything a finished trip left behind.
 */
@Service
public class TripLocationService {

    private final TripLocationRepository locations;

    private final BookingRepository bookings;

    private final Duration staleAfter;

    public TripLocationService(
            TripLocationRepository locations,
            BookingRepository bookings,
            @Value("${yocabs.tracking.stale-after-minutes:180}") long staleAfterMinutes
    ) {
        this.locations = locations;
        this.bookings = bookings;
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

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Location can only be shared while the trip is in progress"
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

    /** The partner running the trip, or an administrator, sees where the car is. */
    @Transactional(readOnly = true)
    public Optional<TripLocation> latest(
            Actor actor,
            UUID bookingId
    ) {
        Booking booking = requireBooking(bookingId);

        requireWatcher(actor, booking);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            return Optional.empty();
        }

        return locations.findByBookingId(bookingId);
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

        locations.deleteRecordedBefore(
                Instant.now().minus(staleAfter)
        );
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

    /** Only the partner running the trip and administrators may watch a driver. */
    private void requireWatcher(Actor actor, Booking booking) {

        if (actor.isAdmin()) {
            return;
        }

        if (actor.isPartnerUser()) {
            actor.requirePartnerAccess(booking.getTravelPartnerId());
            return;
        }

        throw new AccessDeniedException(
                "Only the travel partner and administrators can follow a trip"
        );
    }

    public record LiveTrip(
            Booking booking,
            Optional<TripLocation> location
    ) {
    }
}
