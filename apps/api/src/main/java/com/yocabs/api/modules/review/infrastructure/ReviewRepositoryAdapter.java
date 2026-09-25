package com.yocabs.api.modules.review.infrastructure;

import com.yocabs.api.modules.review.domain.Review;
import com.yocabs.api.modules.review.domain.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewJpaRepository jpa;

    public ReviewRepositoryAdapter(ReviewJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Review create(Review review) {
        return jpa.saveAndFlush(ReviewEntity.fromDomain(review)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findByBookingId(UUID bookingId) {
        return jpa.findByBookingId(bookingId).map(ReviewEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByTravelPartnerId(UUID travelPartnerId, int limit) {
        return jpa.findByTravelPartnerIdOrderByCreatedAtDesc(
                        travelPartnerId, PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream().map(ReviewEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummary summarize(UUID travelPartnerId) {
        Object[] row = jpa.summarize(travelPartnerId).getFirst();
        double average = ((Number) row[0]).doubleValue();
        return new RatingSummary(Math.round(average * 10.0) / 10.0, ((Number) row[1]).longValue());
    }
}
