package com.yocabs.api.modules.touristfeedback.domain;

import java.util.Optional;
import java.util.UUID;

public interface TouristFeedbackRepository {

    Optional<TouristFeedback> findByBookingId(UUID bookingId);

    TouristFeedback create(TouristFeedback feedback);
}
