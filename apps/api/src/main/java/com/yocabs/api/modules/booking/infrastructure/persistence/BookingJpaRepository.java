package com.yocabs.api.modules.booking.infrastructure.persistence;

import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingJpaRepository
        extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByTouristIdAndIdempotencyKey(UUID touristId, String idempotencyKey);

    List<BookingEntity> findByTouristIdOrderByCreatedAtDesc(UUID touristId);

    List<BookingEntity> findByTravelPartnerIdOrderByCreatedAtDesc(UUID travelPartnerId);

    List<BookingEntity> findByTravelPartnerIdAndStatusOrderByCreatedAtDesc(
            UUID travelPartnerId,
            BookingStatus status
    );

    List<BookingEntity> findByDriverIdOrderByStartDateAsc(UUID driverId);

    List<BookingEntity> findByStatusOrderByCreatedAtDesc(BookingStatus status, Pageable pageable);

    List<BookingEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select count(b) > 0 from BookingEntity b "
            + "where b.tripRequestId = :tripRequestId "
            + "and (b.status in :liveStatuses "
            + "or (b.status = :pending and b.holdExpiresAt > :now))")
    boolean existsLiveForTripRequest(
            @Param("tripRequestId") UUID tripRequestId,
            @Param("liveStatuses") Collection<BookingStatus> liveStatuses,
            @Param("pending") BookingStatus pending,
            @Param("now") Instant now
    );

    @Query("select count(b) > 0 from BookingEntity b "
            + "where b.vehicleId = :vehicleId "
            + "and b.id <> :excludeId "
            + "and b.startDate <= :endDate and b.endDate >= :startDate "
            + "and (b.status in :blockingStatuses "
            + "or (b.status = :pending and b.holdExpiresAt > :now))")
    boolean existsBlockingForVehicle(
            @Param("vehicleId") UUID vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") UUID excludeId,
            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses,
            @Param("pending") BookingStatus pending,
            @Param("now") Instant now
    );

    @Query("select count(b) > 0 from BookingEntity b "
            + "where b.driverId = :driverId "
            + "and b.id <> :excludeId "
            + "and b.startDate <= :endDate and b.endDate >= :startDate "
            + "and b.status in :blockingStatuses")
    boolean existsBlockingForDriver(
            @Param("driverId") UUID driverId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") UUID excludeId,
            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses
    );

    @Query("select b from BookingEntity b "
            + "where b.status = :pending and b.holdExpiresAt <= :now")
    List<BookingEntity> findExpiredHolds(
            @Param("pending") BookingStatus pending,
            @Param("now") Instant now
    );

    @Query("select b from BookingEntity b "
            + "where b.tripRequestId = :tripRequestId "
            + "and b.status = :pending and b.holdExpiresAt <= :now")
    List<BookingEntity> findExpiredHoldsForTripRequest(
            @Param("tripRequestId") UUID tripRequestId,
            @Param("pending") BookingStatus pending,
            @Param("now") Instant now
    );

    @Query("select b.status, count(b) from BookingEntity b group by b.status")
    List<Object[]> countByStatus();

    @Query("select coalesce(sum(b.totalAmount), 0), coalesce(sum(b.commissionAmount), 0) "
            + "from BookingEntity b where b.status in :statuses")
    List<Object[]> sumRevenue(@Param("statuses") Collection<BookingStatus> statuses);
}
