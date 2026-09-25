package com.yocabs.api.modules.booking.infrastructure.persistence;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class BookingRepositoryAdapter implements BookingRepository {

    private static final UUID NONE = new UUID(0L, 0L);

    private static final Set<BookingStatus> LIVE_FOR_TRIP_REQUEST =
            Set.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);

    private static final Set<BookingStatus> BLOCKING =
            Set.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);

    private static final Set<BookingStatus> REVENUE =
            Set.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);

    private final BookingJpaRepository jpa;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingRepositoryAdapter(BookingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Booking create(Booking booking) {
        return jpa.saveAndFlush(BookingEntity.fromDomain(booking)).toDomain();
    }

    @Override
    @Transactional
    public Booking update(Booking booking) {
        BookingEntity entity =
                jpa.findById(booking.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Booking not found: " + booking.getId()));

        entity.updateFromDomain(booking);
        jpa.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findById(UUID id) {
        return jpa.findById(id).map(BookingEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findByTouristIdAndIdempotencyKey(UUID touristId, String idempotencyKey) {
        return jpa.findByTouristIdAndIdempotencyKey(touristId, idempotencyKey)
                .map(BookingEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByTouristId(UUID touristId) {
        return jpa.findByTouristIdOrderByCreatedAtDesc(touristId).stream()
                .map(BookingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByPartnerId(UUID travelPartnerId, BookingStatus status) {
        return (status == null
                ? jpa.findByTravelPartnerIdOrderByCreatedAtDesc(travelPartnerId)
                : jpa.findByTravelPartnerIdAndStatusOrderByCreatedAtDesc(travelPartnerId, status))
                .stream().map(BookingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByDriverId(UUID driverId) {
        return jpa.findByDriverIdOrderByStartDateAsc(driverId).stream()
                .map(BookingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findAll(BookingStatus status, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
        return (status == null
                ? jpa.findAllByOrderByCreatedAtDesc(page)
                : jpa.findByStatusOrderByCreatedAtDesc(status, page))
                .stream().map(BookingEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void lockVehicle(UUID vehicleId) {
        entityManager
                .createNativeQuery("select id from vehicles where id = :id for update")
                .setParameter("id", vehicleId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsLiveBookingForTripRequest(UUID tripRequestId, Instant now) {
        return jpa.existsLiveForTripRequest(
                tripRequestId, LIVE_FOR_TRIP_REQUEST, BookingStatus.PENDING_PAYMENT, now
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBlockingBookingForVehicle(
            UUID vehicleId,
            LocalDate startDate,
            LocalDate endDate,
            Instant now,
            UUID excludeBookingId
    ) {
        return jpa.existsBlockingForVehicle(
                vehicleId, startDate, endDate,
                excludeBookingId == null ? NONE : excludeBookingId,
                BLOCKING, BookingStatus.PENDING_PAYMENT, now
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBlockingBookingForDriver(
            UUID driverId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludeBookingId
    ) {
        return jpa.existsBlockingForDriver(
                driverId, startDate, endDate,
                excludeBookingId == null ? NONE : excludeBookingId, BLOCKING
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findExpiredHolds(Instant now) {
        return jpa.findExpiredHolds(BookingStatus.PENDING_PAYMENT, now).stream()
                .map(BookingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findExpiredHoldsForTripRequest(UUID tripRequestId, Instant now) {
        return jpa.findExpiredHoldsForTripRequest(tripRequestId, BookingStatus.PENDING_PAYMENT, now)
                .stream().map(BookingEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStats stats() {

        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        long total = 0;

        for (BookingStatus status : BookingStatus.values()) {
            byStatus.put(status, 0L);
        }

        for (Object[] row : jpa.countByStatus()) {
            long count = ((Number) row[1]).longValue();
            byStatus.put((BookingStatus) row[0], count);
            total += count;
        }

        Object[] revenue = jpa.sumRevenue(REVENUE).getFirst();

        return new BookingStats(
                total,
                byStatus,
                (BigDecimal) revenue[0],
                (BigDecimal) revenue[1]
        );
    }
}
