package com.yocabs.api.modules.tracking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TripLocationJpaRepository
        extends JpaRepository<TripLocationEntity, UUID> {

    List<TripLocationEntity> findByBookingIdIn(Collection<UUID> bookingIds);

    int deleteByRecordedAtBefore(java.time.Instant cutoff);
}
