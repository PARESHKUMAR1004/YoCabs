package com.yocabs.api.modules.tracking.infrastructure.persistence;

import com.yocabs.api.modules.tracking.domain.model.TripLocation;
import com.yocabs.api.modules.tracking.domain.repository.TripLocationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TripLocationRepositoryAdapter
        implements TripLocationRepository {

    private final TripLocationJpaRepository jpaRepository;

    public TripLocationRepositoryAdapter(
            TripLocationJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public TripLocation save(TripLocation location) {

        TripLocationEntity entity =
                jpaRepository
                        .findById(location.bookingId())
                        .orElseGet(() ->
                                TripLocationEntity.fromDomain(location)
                        );

        entity.apply(location);

        return jpaRepository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TripLocation> findByBookingId(UUID bookingId) {
        return jpaRepository
                .findById(bookingId)
                .map(TripLocationEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripLocation> findByBookingIdIn(
            Collection<UUID> bookingIds
    ) {
        if (bookingIds.isEmpty()) {
            return List.of();
        }

        return jpaRepository
                .findByBookingIdIn(bookingIds)
                .stream()
                .map(TripLocationEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByBookingId(UUID bookingId) {
        jpaRepository.deleteById(bookingId);
    }

    @Override
    @Transactional
    public int deleteRecordedBefore(java.time.Instant cutoff) {
        return jpaRepository.deleteByRecordedAtBefore(cutoff);
    }
}
