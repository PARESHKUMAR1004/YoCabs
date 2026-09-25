package com.yocabs.api.modules.review.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {

    Review create(Review review);

    Optional<Review> findByBookingId(UUID bookingId);

    List<Review> findByTravelPartnerId(UUID travelPartnerId, int limit);

    RatingSummary summarize(UUID travelPartnerId);

    record RatingSummary(double average, long count) {
    }
}
