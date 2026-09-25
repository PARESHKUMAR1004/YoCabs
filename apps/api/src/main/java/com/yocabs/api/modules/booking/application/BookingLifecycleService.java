package com.yocabs.api.modules.booking.application;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.driver.domain.model.Driver;
import com.yocabs.api.modules.driver.domain.repository.DriverRepository;
import com.yocabs.api.shared.events.BookingCompleted;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Operational states of a confirmed booking: driver assignment, start, completion. */
@Service
public class BookingLifecycleService {

    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final DriverRepository drivers;
    private final ApplicationEventPublisher events;
    private final ZoneId zone;

    public BookingLifecycleService(
            BookingRepository bookings,
            BookingService bookingService,
            DriverRepository drivers,
            ApplicationEventPublisher events,
            @Value("${yocabs.booking.zone:Asia/Kolkata}") String zone
    ) {
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.drivers = drivers;
        this.events = events;
        this.zone = ZoneId.of(zone);
    }

    @Transactional
    public Booking assignDriver(Actor actor, UUID bookingId, UUID driverId) {

        Booking booking = bookingService.get(actor, bookingId);

        if (!actor.isPartnerUser() && !actor.isAdmin()) {
            throw new AccessDeniedException("Only the travel partner can assign a driver");
        }

        Driver driver =
                drivers.findById(driverId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Driver not found: " + driverId));

        if (!driver.getTravelPartnerId().equals(booking.getTravelPartnerId())) {
            throw new IllegalArgumentException("Driver does not belong to this travel partner");
        }

        if (!driver.isActive()) {
            throw new IllegalStateException("The driver is not active");
        }

        if (bookings.existsBlockingBookingForDriver(
                driverId, booking.getStartDate(), booking.getEndDate(), booking.getId())) {
            throw new IllegalStateException(
                    "The driver is already assigned to an overlapping trip"
            );
        }

        booking.assignDriver(driverId, Instant.now());
        Booking saved = bookings.update(booking);

        events.publishEvent(
                NotificationRequested.toUser(
                        driverId,
                        "TRIP_ASSIGNED",
                        "New trip assigned",
                        "You have a trip from " + saved.getPickupDescription() + " to "
                                + saved.getDestinationDescription() + " on " + saved.getStartDate() + ".",
                        "BOOKING",
                        saved.getId()
                )
        );

        events.publishEvent(
                NotificationRequested.toUser(
                        saved.getTouristId(),
                        "DRIVER_ASSIGNED",
                        "Driver assigned",
                        driver.getName() + " will drive your trip.",
                        "BOOKING",
                        saved.getId()
                )
        );

        return saved;
    }

    @Transactional
    public Booking startTrip(Actor actor, UUID bookingId, String code) {

        Booking booking = requireOperator(actor, bookingId);

        // Admins can start a trip for support cases; everyone else needs the tourist's code.
        booking.startTrip(
                LocalDate.now(zone),
                actor.isAdmin() ? booking.getStartCode() : code,
                Instant.now());
        Booking saved = bookings.update(booking);

        events.publishEvent(
                NotificationRequested.toUser(
                        saved.getTouristId(),
                        "TRIP_STARTED",
                        "Your trip has started",
                        "Have a safe journey!",
                        "BOOKING",
                        saved.getId()
                )
        );

        return saved;
    }

    @Transactional
    public Booking completeTrip(Actor actor, UUID bookingId) {

        Booking booking = requireOperator(actor, bookingId);

        booking.completeTrip(Instant.now());
        Booking saved = bookings.update(booking);

        events.publishEvent(
                new BookingCompleted(
                        saved.getId(),
                        saved.getTravelPartnerId(),
                        saved.getTouristId(),
                        saved.getCurrency(),
                        saved.getTotalAmount(),
                        saved.getTokenAmount(),
                        saved.getCommissionAmount()
                )
        );

        events.publishEvent(
                NotificationRequested.toUser(
                        saved.getTouristId(),
                        "TRIP_COMPLETED",
                        "Trip completed",
                        "Thanks for travelling with YoCabs. Please rate your trip.",
                        "BOOKING",
                        saved.getId()
                )
        );

        return saved;
    }

    /** Only the assigned driver, the owning partner's users, or an admin may operate a trip. */
    private Booking requireOperator(Actor actor, UUID bookingId) {

        Booking booking = bookingService.get(actor, bookingId);

        if (actor.role() == Role.TOURIST) {
            throw new AccessDeniedException("Tourists cannot operate a trip");
        }

        return booking;
    }
}
