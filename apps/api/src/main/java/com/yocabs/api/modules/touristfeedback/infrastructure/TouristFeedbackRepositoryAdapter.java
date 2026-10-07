package com.yocabs.api.modules.touristfeedback.infrastructure;

import com.yocabs.api.modules.touristfeedback.domain.TouristFeedback;
import com.yocabs.api.modules.touristfeedback.domain.TouristFeedbackRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TouristFeedbackRepositoryAdapter implements TouristFeedbackRepository {

    private final TouristFeedbackJpaRepository jpa;

    public TouristFeedbackRepositoryAdapter(TouristFeedbackJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TouristFeedback> findByBookingId(UUID bookingId) {
        return jpa.findByBookingId(bookingId).map(TouristFeedbackEntity::toDomain);
    }

    @Override
    @Transactional
    public TouristFeedback create(TouristFeedback feedback) {
        return jpa.saveAndFlush(TouristFeedbackEntity.fromDomain(feedback)).toDomain();
    }
}
