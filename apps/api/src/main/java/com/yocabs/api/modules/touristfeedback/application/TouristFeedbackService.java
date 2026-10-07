package com.yocabs.api.modules.touristfeedback.application;

import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.payment.domain.model.PaymentPurpose;
import com.yocabs.api.modules.payment.domain.repository.PaymentRepository;
import com.yocabs.api.modules.touristfeedback.domain.TouristFeedback;
import com.yocabs.api.modules.touristfeedback.domain.TouristFeedbackRepository;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A driver's rating of the traveller: only available once a trip is completed and the fare is
 * fully settled, mirroring (in reverse) the tourist-rates-partner flow in the review module.
 */
@Service
public class TouristFeedbackService {

    private final TouristFeedbackRepository feedback;
    private final BookingService bookingService;
    private final PaymentRepository payments;

    public TouristFeedbackService(
            TouristFeedbackRepository feedback,
            BookingService bookingService,
            PaymentRepository payments
    ) {
        this.feedback = feedback;
        this.bookingService = bookingService;
        this.payments = payments;
    }

    @Transactional
    public TouristFeedback submit(Actor actor, UUID bookingId, int rating, String comment) {

        actor.requireRole(Role.DRIVER);

        // Also confirms this driver is the one assigned to the booking.
        Booking booking = bookingService.get(actor, bookingId);

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Only a completed trip can be rated");
        }

        if (!isSettled(booking)) {
            throw new IllegalStateException("The trip balance must be settled before leaving feedback");
        }

        if (feedback.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("This trip has already been rated");
        }

        return feedback.create(
                TouristFeedback.create(bookingId, actor.userId(), booking.getTouristId(), rating, comment)
        );
    }

    /** Whether there is nothing left to collect: no balance owed, or the balance was paid online. */
    @Transactional(readOnly = true)
    public boolean isSettled(Booking booking) {
        boolean balanceOwed = booking.getTotalAmount().subtract(booking.getTokenAmount()).signum() > 0;
        return !balanceOwed
                || payments.findPaidByBookingId(booking.getId(), PaymentPurpose.BALANCE).isPresent();
    }
}
