package com.yocabs.api.modules.booking.domain.repository;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    Booking create(Booking booking);

    Booking update(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByTouristIdAndIdempotencyKey(UUID touristId, String idempotencyKey);

    List<Booking> findByTouristId(UUID touristId);

    List<Booking> findByPartnerId(UUID travelPartnerId, BookingStatus status);

    List<Booking> findByDriverId(UUID driverId);

    List<Booking> findAll(BookingStatus status, int limit);

    /** Serialises concurrent bookings of the same vehicle (row lock held until commit). */
    void lockVehicle(UUID vehicleId);

    boolean existsLiveBookingForTripRequest(UUID tripRequestId, Instant now);

    boolean existsBlockingBookingForVehicle(
            UUID vehicleId,
            LocalDate startDate,
            LocalDate endDate,
            Instant now,
            UUID excludeBookingId
    );

    boolean existsBlockingBookingForDriver(
            UUID driverId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludeBookingId
    );

    List<Booking> findExpiredHolds(Instant now);

    List<Booking> findExpiredHoldsForTripRequest(UUID tripRequestId, Instant now);

    BookingStats stats();

    record BookingStats(
            long total,
            Map<BookingStatus, Long> byStatus,
            BigDecimal grossValue,
            BigDecimal commission
    ) {
    }
}
