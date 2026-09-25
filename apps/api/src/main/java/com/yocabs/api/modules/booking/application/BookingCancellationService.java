package com.yocabs.api.modules.booking.application;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.payment.application.PaymentService;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Cancels a booking and applies the refund policy:
 * partner/admin cancellations always refund the token; a tourist cancelling
 * inside the free-cancellation window is refunded, later cancellations are not.
 */
@Service
public class BookingCancellationService {

    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher events;
    private final Duration freeCancellationWindow;
    private final ZoneId zone;

    public BookingCancellationService(
            BookingRepository bookings,
            BookingService bookingService,
            PaymentService paymentService,
            ApplicationEventPublisher events,
            @Value("${yocabs.booking.free-cancellation-hours:24}") long freeCancellationHours,
            @Value("${yocabs.booking.zone:Asia/Kolkata}") String zone
    ) {
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.events = events;
        this.freeCancellationWindow = Duration.ofHours(freeCancellationHours);
        this.zone = ZoneId.of(zone);
    }

    @Transactional
    public Booking cancel(Actor actor, UUID bookingId, String reason) {

        Booking booking = bookingService.get(actor, bookingId);

        if (actor.role() == Role.DRIVER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Drivers cannot cancel bookings"
            );
        }

        Instant now = Instant.now();
        boolean wasPaid = booking.getStatus() == BookingStatus.CONFIRMED;

        booking.cancel(reason, actor.role(), now);
        Booking saved = bookings.update(booking);

        if (wasPaid && refundDue(actor, saved, now)) {
            paymentService.refund(saved.getId(), saved.getTokenAmount(), "cancellation");
        }

        UUID notifyTourist = saved.getTouristId();

        if (actor.role() != Role.TOURIST) {
            events.publishEvent(
                    NotificationRequested.toUser(
                            notifyTourist,
                            "BOOKING_CANCELLED",
                            "Booking cancelled",
                            "Your booking was cancelled by the travel partner. Any token paid is refunded.",
                            "BOOKING",
                            saved.getId()
                    )
            );
        }

        if (!actor.isPartnerUser()) {
            events.publishEvent(
                    NotificationRequested.toPartner(
                            saved.getTravelPartnerId(),
                            "BOOKING_CANCELLED",
                            "Booking cancelled",
                            "A booking starting " + saved.getStartDate() + " was cancelled.",
                            "BOOKING",
                            saved.getId()
                    )
            );
        }

        return saved;
    }

    private boolean refundDue(Actor actor, Booking booking, Instant now) {

        if (actor.role() != Role.TOURIST) {
            return true;
        }

        Instant tripStart = booking.getStartDate().atStartOfDay(zone).toInstant();

        return !now.isAfter(tripStart.minus(freeCancellationWindow));
    }
}
