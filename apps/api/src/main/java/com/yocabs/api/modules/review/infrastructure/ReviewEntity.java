package com.yocabs.api.modules.review.infrastructure;

import com.yocabs.api.modules.review.domain.Review;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "tourist_id", nullable = false)
    private UUID touristId;

    @Column(name = "travel_partner_id", nullable = false)
    private UUID travelPartnerId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReviewEntity() {
        // JPA
    }

    static ReviewEntity fromDomain(Review review) {
        ReviewEntity entity = new ReviewEntity();
        entity.id = review.id();
        entity.bookingId = review.bookingId();
        entity.touristId = review.touristId();
        entity.travelPartnerId = review.travelPartnerId();
        entity.driverId = review.driverId();
        entity.rating = (short) review.rating();
        entity.comment = review.comment();
        entity.createdAt = review.createdAt();
        return entity;
    }

    Review toDomain() {
        return new Review(
                id, bookingId, touristId, travelPartnerId, driverId, rating, comment, createdAt
        );
    }
}
