package com.yocabs.api.modules.review.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewJpaRepository
        extends JpaRepository<ReviewEntity, UUID> {

    Optional<ReviewEntity> findByBookingId(UUID bookingId);

    List<ReviewEntity> findByTravelPartnerIdOrderByCreatedAtDesc(UUID travelPartnerId, Pageable pageable);

    @Query("select coalesce(avg(r.rating), 0), count(r) from ReviewEntity r "
            + "where r.travelPartnerId = :partnerId")
    List<Object[]> summarize(@Param("partnerId") UUID partnerId);
}
