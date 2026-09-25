package com.yocabs.api.modules.booking.application;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.negotiation.application.NegotiationService;
import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.domain.PriceComponent;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripRequestStatus;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.modules.tripsearch.application.SelectedOptionService;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BookingRepository bookings;
    private final TripRequestRepository tripRequests;
    private final SelectedOptionService selectedOptionService;
    private final NegotiationService negotiationService;
    private final ApplicationEventPublisher events;
    private final BigDecimal tokenPercentage;
    private final BigDecimal commissionPercentage;
    private final Duration holdDuration;

    public BookingService(
            BookingRepository bookings,
            TripRequestRepository tripRequests,
            SelectedOptionService selectedOptionService,
            NegotiationService negotiationService,
            ApplicationEventPublisher events,
            @Value("${yocabs.booking.token-percentage:25}") BigDecimal tokenPercentage,
            @Value("${yocabs.booking.commission-percentage:10}") BigDecimal commissionPercentage,
            @Value("${yocabs.booking.hold-minutes:15}") long holdMinutes
    ) {
        this.bookings = bookings;
        this.tripRequests = tripRequests;
        this.selectedOptionService = selectedOptionService;
        this.negotiationService = negotiationService;
        this.events = events;
        this.tokenPercentage = tokenPercentage;
        this.commissionPercentage = commissionPercentage;
        this.holdDuration = Duration.ofMinutes(holdMinutes);
    }

    @Transactional
    public Booking createBooking(
            Actor actor,
            UUID tripRequestId,
            UUID vehicleId,
            TripType tripType,
            UUID negotiationId,
            String idempotencyKey
    ) {
        actor.requireRole(Role.TOURIST);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("An Idempotency-Key is required");
        }

        Booking replay =
                bookings.findByTouristIdAndIdempotencyKey(actor.userId(), idempotencyKey).orElse(null);

        if (replay != null) {
            if (!replay.getTripRequestId().equals(tripRequestId)
                    || !replay.getVehicleId().equals(vehicleId)
                    || replay.getTripType() != tripType) {
                throw new IllegalStateException(
                        "This Idempotency-Key was already used for a different booking"
                );
            }
            return replay;
        }

        TripRequest tripRequest =
                tripRequests.findById(new TripRequestId(tripRequestId))
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Trip request not found: " + tripRequestId));

        actor.requireTouristIs(tripRequest.getTouristId().value());

        if (tripRequest.getStatus() != TripRequestStatus.SUBMITTED) {
            throw new IllegalStateException("Only a submitted trip request can be booked");
        }

        Instant now = Instant.now();

        // Serialise concurrent bookings of this vehicle before checking availability.
        bookings.lockVehicle(vehicleId);

        for (Booking stale : bookings.findExpiredHoldsForTripRequest(tripRequestId, now)) {
            stale.expire(now);
            bookings.update(stale);
        }

        if (bookings.existsLiveBookingForTripRequest(tripRequestId, now)) {
            throw new IllegalStateException("This trip already has an active booking");
        }

        // Availability, eligibility and price are all re-validated at booking time.
        PricedTravelOption option =
                selectedOptionService.resolve(tripRequest, vehicleId, tripType);

        var dates = tripRequest.getTravelDateRange();

        if (bookings.existsBlockingBookingForVehicle(
                vehicleId, dates.startDate(), dates.endDate(), now, null)) {
            throw new IllegalStateException(
                    "The vehicle is no longer available for the selected dates"
            );
        }

        BigDecimal listed = option.price().totalAmount();
        BigDecimal total = listed;
        List<PriceComponent> components = new ArrayList<>(option.price().components());

        if (negotiationId != null) {
            BigDecimal agreed =
                    negotiationService.consumeAgreedPrice(
                            negotiationId, actor.userId(), tripRequestId, vehicleId, tripType
                    );

            if (agreed.compareTo(listed) < 0) {
                components.add(
                        new PriceComponent(
                                "NEGOTIATED_DISCOUNT",
                                "Negotiated discount",
                                agreed.subtract(listed)
                        )
                );
                total = agreed;
            }
        }

        Booking booking =
                bookings.create(
                        Booking.create(
                                tripRequestId,
                                actor.userId(),
                                option.travelPartner().getId(),
                                vehicleId,
                                negotiationId,
                                tripType,
                                dates.startDate(),
                                dates.endDate(),
                                tripRequest.getPassengerCount().value(),
                                tripRequest.getItinerary().pickup().description(),
                                tripRequest.getItinerary().destination().description(),
                                option.price().currency(),
                                total,
                                percentage(total, tokenPercentage),
                                percentage(total, commissionPercentage),
                                components,
                                now.plus(holdDuration),
                                idempotencyKey
                        )
                );

        log.info("Booking {} created for trip request {} (hold until {})",
                booking.getId(), tripRequestId, booking.getHoldExpiresAt());

        return booking;
    }

    /**
     * Called once the token payment has been verified. Returns false when the
     * booking can no longer be honoured (the caller must then refund).
     */
    @Transactional
    public boolean confirmAfterPayment(UUID bookingId) {

        Booking booking = load(bookingId);
        Instant now = Instant.now();

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return true;
        }

        boolean expired = booking.getStatus() == BookingStatus.EXPIRED || booking.isHoldExpired(now);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.EXPIRED) {
            return false;
        }

        bookings.lockVehicle(booking.getVehicleId());

        if (expired) {
            if (bookings.existsBlockingBookingForVehicle(
                    booking.getVehicleId(), booking.getStartDate(), booking.getEndDate(),
                    now, booking.getId())
                    || bookings.existsLiveBookingForTripRequest(booking.getTripRequestId(), now)) {
                return false;
            }

            if (booking.getStatus() == BookingStatus.EXPIRED) {
                booking.reviveAndConfirm(now);
            } else {
                booking.confirm(now);
            }
        } else {
            booking.confirm(now);
        }

        Booking saved = bookings.update(booking);

        events.publishEvent(
                NotificationRequested.toUser(
                        saved.getTouristId(),
                        "BOOKING_CONFIRMED",
                        "Booking confirmed",
                        "Your booking for " + saved.getPickupDescription() + " to "
                                + saved.getDestinationDescription() + " is confirmed.",
                        "BOOKING",
                        saved.getId()
                )
        );

        events.publishEvent(
                NotificationRequested.toPartner(
                        saved.getTravelPartnerId(),
                        "BOOKING_RECEIVED",
                        "New confirmed booking",
                        "A booking from " + saved.getPickupDescription() + " to "
                                + saved.getDestinationDescription() + " starting "
                                + saved.getStartDate() + " has been confirmed.",
                        "BOOKING",
                        saved.getId()
                )
        );

        return true;
    }

    @Scheduled(fixedDelayString = "${yocabs.booking.hold-scan-ms:60000}")
    @Transactional
    public void expireStaleHolds() {

        Instant now = Instant.now();
        int expired = 0;

        for (Booking booking : bookings.findExpiredHolds(now)) {
            booking.expire(now);
            bookings.update(booking);

            events.publishEvent(
                    NotificationRequested.toUser(
                            booking.getTouristId(),
                            "BOOKING_HOLD_EXPIRED",
                            "Booking hold expired",
                            "Your booking was not paid in time and has been released.",
                            "BOOKING",
                            booking.getId()
                    )
            );
            expired++;
        }

        if (expired > 0) {
            log.info("Expired {} unpaid booking hold(s)", expired);
        }
    }

    @Transactional(readOnly = true)
    public Booking get(Actor actor, UUID bookingId) {

        Booking booking = load(bookingId);
        requireAccess(actor, booking);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> listMine(Actor actor) {
        actor.requireRole(Role.TOURIST);
        return bookings.findByTouristId(actor.userId());
    }

    @Transactional(readOnly = true)
    public List<Booking> listForPartner(Actor actor, UUID travelPartnerId, BookingStatus status) {
        actor.requirePartnerAccess(travelPartnerId);
        return bookings.findByPartnerId(travelPartnerId, status);
    }

    @Transactional(readOnly = true)
    public List<Booking> listAll(Actor actor, BookingStatus status, int limit) {
        actor.requireAdmin();
        return bookings.findAll(status, limit);
    }

    @Transactional(readOnly = true)
    public List<Booking> listForDriver(Actor actor) {
        actor.requireRole(Role.DRIVER);
        return bookings.findByDriverId(actor.userId());
    }

    public void requireAccess(Actor actor, Booking booking) {

        if (actor.isAdmin()) {
            return;
        }

        switch (actor.role()) {
            case TOURIST -> actor.requireTouristIs(booking.getTouristId());
            case PARTNER_OWNER, PARTNER_STAFF -> actor.requirePartnerAccess(booking.getTravelPartnerId());
            case DRIVER -> {
                if (!actor.userId().equals(booking.getDriverId())) {
                    throw new AccessDeniedException("Not your assigned booking");
                }
            }
            default -> throw new AccessDeniedException("Not permitted");
        }
    }

    Booking load(UUID bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    private static BigDecimal percentage(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
