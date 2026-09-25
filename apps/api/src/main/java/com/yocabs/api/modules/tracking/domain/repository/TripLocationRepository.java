package com.yocabs.api.modules.tracking.domain.repository;

import com.yocabs.api.modules.tracking.domain.model.TripLocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripLocationRepository {

    /** Inserts or overwrites the single row for this booking. */
    TripLocation save(TripLocation location);

    Optional<TripLocation> findByBookingId(UUID bookingId);

    /** One query for a whole list of trips, so a live map never goes booking by booking. */
    List<TripLocation> findByBookingIdIn(Collection<UUID> bookingIds);

    void deleteByBookingId(UUID bookingId);

    /** Clears positions nobody has updated since the cutoff; returns how many went. */
    int deleteRecordedBefore(java.time.Instant cutoff);
}
