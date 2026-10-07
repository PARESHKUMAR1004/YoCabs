package com.yocabs.api.modules.touristfeedback.domain;

import java.time.Instant;
import java.util.UUID;

/** A driver's rating of the traveller on one completed, paid-up trip. */
public record TouristFeedback(
        UUID id,
        UUID bookingId,
        UUID driverId,
        UUID touristId,
        int rating,
        String comment,
        Instant createdAt
) {

    public TouristFeedback {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (comment != null && comment.length() > 2000) {
            throw new IllegalArgumentException("Feedback comment is too long");
        }
    }

    public static TouristFeedback create(
            UUID bookingId, UUID driverId, UUID touristId, int rating, String comment
    ) {
        return new TouristFeedback(
                UUID.randomUUID(), bookingId, driverId, touristId, rating,
                comment == null || comment.isBlank() ? null : comment.trim(), Instant.now()
        );
    }
}
