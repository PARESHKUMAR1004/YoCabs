package com.yocabs.api.modules.review.application;

import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.review.domain.Review;
import com.yocabs.api.modules.review.domain.ReviewRepository;
import com.yocabs.api.modules.review.domain.ReviewRepository.RatingSummary;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final BookingService bookingService;
    private final ApplicationEventPublisher events;

    public ReviewService(
            ReviewRepository reviews,
            BookingService bookingService,
            ApplicationEventPublisher events
    ) {
        this.reviews = reviews;
        this.bookingService = bookingService;
        this.events = events;
    }

    @Transactional
    public Review submit(Actor actor, UUID bookingId, int rating, String comment) {

        actor.requireRole(Role.TOURIST);

        Booking booking = bookingService.get(actor, bookingId);

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Only a completed trip can be reviewed");
        }

        if (reviews.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("This trip has already been reviewed");
        }

        Review review =
                reviews.create(
                        Review.create(
                                bookingId,
                                actor.userId(),
                                booking.getTravelPartnerId(),
                                booking.getDriverId(),
                                rating,
                                comment
                        )
                );

        events.publishEvent(
                NotificationRequested.toPartner(
                        booking.getTravelPartnerId(),
                        "REVIEW_RECEIVED",
                        "New review",
                        "A tourist rated a completed trip " + rating + "/5.",
                        "BOOKING",
                        bookingId
                )
        );

        return review;
    }

    @Transactional(readOnly = true)
    public List<Review> listForPartner(UUID travelPartnerId, int limit) {
        return reviews.findByTravelPartnerId(travelPartnerId, limit);
    }

    @Transactional(readOnly = true)
    public RatingSummary summary(UUID travelPartnerId) {
        return reviews.summarize(travelPartnerId);
    }
}
