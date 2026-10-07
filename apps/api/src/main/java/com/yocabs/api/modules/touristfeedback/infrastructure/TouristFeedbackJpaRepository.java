package com.yocabs.api.modules.touristfeedback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TouristFeedbackJpaRepository extends JpaRepository<TouristFeedbackEntity, UUID> {

    Optional<TouristFeedbackEntity> findByBookingId(UUID bookingId);
}
