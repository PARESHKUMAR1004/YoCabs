package com.yocabs.api.modules.touristfeedback.infrastructure;

import com.yocabs.api.modules.touristfeedback.domain.TouristFeedback;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tourist_feedback")
public class TouristFeedbackEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "tourist_id", nullable = false)
    private UUID touristId;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TouristFeedbackEntity() {
        // JPA
    }

    static TouristFeedbackEntity fromDomain(TouristFeedback feedback) {
        TouristFeedbackEntity entity = new TouristFeedbackEntity();
        entity.id = feedback.id();
        entity.bookingId = feedback.bookingId();
        entity.driverId = feedback.driverId();
        entity.touristId = feedback.touristId();
        entity.rating = (short) feedback.rating();
        entity.comment = feedback.comment();
        entity.createdAt = feedback.createdAt();
        return entity;
    }

    TouristFeedback toDomain() {
        return new TouristFeedback(id, bookingId, driverId, touristId, rating, comment, createdAt);
    }
}
