package com.yocabs.api.modules.review.domain;

import java.time.Instant;
import java.util.UUID;

public record Review(
        UUID id,
        UUID bookingId,
        UUID touristId,
        UUID travelPartnerId,
        UUID driverId,
        int rating,
        String comment,
        Instant createdAt
) {

    public Review {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (comment != null && comment.length() > 2000) {
            throw new IllegalArgumentException("Review comment is too long");
        }
    }

    public static Review create(
            UUID bookingId,
            UUID touristId,
            UUID travelPartnerId,
            UUID driverId,
            int rating,
            String comment
    ) {
        return new Review(
                UUID.randomUUID(), bookingId, touristId, travelPartnerId, driverId, rating,
                comment == null || comment.isBlank() ? null : comment.trim(), Instant.now()
        );
    }
}
