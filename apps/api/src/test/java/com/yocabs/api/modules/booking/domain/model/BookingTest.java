package com.yocabs.api.modules.booking.domain.model;

import com.yocabs.api.modules.pricing.domain.PriceComponent;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.shared.security.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

    private Booking booking(Instant holdExpiresAt) {
        return Booking.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                TripType.CHAUFFEUR_ONE_WAY, DAY, DAY, 4, "A", "B", "INR",
                new BigDecimal("1000.00"), new BigDecimal("50.00"), new BigDecimal("100.00"),
                List.of(new PriceComponent("BASE", "Base", new BigDecimal("1000.00"))),
                holdExpiresAt, UUID.randomUUID().toString()
        );
    }

    private Booking pending() {
        return booking(Instant.now().plusSeconds(900));
    }

    @Test
    void aNewBookingAwaitsPaymentAndReservesTheVehicleWhileTheHoldIsLive() {
        Booking booking = pending();

        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        assertTrue(booking.blocksVehicle(Instant.now()));
    }

    @Test
    void anExpiredHoldNoLongerReservesTheVehicle() {
        Booking booking = booking(Instant.now().minusSeconds(1));

        assertTrue(booking.isHoldExpired(Instant.now()));
        assertFalse(booking.blocksVehicle(Instant.now()));

        booking.expire(Instant.now());
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    @Test
    void aLiveHoldCannotBeExpired() {
        assertThrows(IllegalStateException.class, () -> pending().expire(Instant.now()));
    }

    @Test
    void confirmationReleasesTheHoldAndKeepsTheVehicleReserved() {
        Booking booking = pending();

        booking.confirm(Instant.now());

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(null, booking.getHoldExpiresAt());
        assertTrue(booking.blocksVehicle(Instant.now().plusSeconds(86_400)));
    }

    @Test
    void anExpiredBookingCanBeRevivedByALatePayment() {
        Booking booking = booking(Instant.now().minusSeconds(1));
        booking.expire(Instant.now());

        booking.reviveAndConfirm(Instant.now());

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertThrows(IllegalStateException.class, () -> pending().reviveAndConfirm(Instant.now()));
    }

    @Test
    void cancellingIsOnlyPossibleBeforeTheTripStarts() {
        Booking pending = pending();
        pending.cancel("changed plans", Role.TOURIST, Instant.now());
        assertEquals(BookingStatus.CANCELLED, pending.getStatus());
        assertEquals(Role.TOURIST, pending.getCancelledByRole());

        Booking started = pending();
        started.confirm(Instant.now());
        started.assignDriver(UUID.randomUUID(), Instant.now());
        started.startTrip(DAY, started.getStartCode(), Instant.now());
        assertThrows(IllegalStateException.class,
                () -> started.cancel("late", Role.TOURIST, Instant.now()));
    }

    @Test
    void aDriverCanOnlyBeAssignedToAConfirmedBooking() {
        assertThrows(IllegalStateException.class,
                () -> pending().assignDriver(UUID.randomUUID(), Instant.now()));
    }

    @Test
    void aTripNeedsADriverAndCannotStartBeforeItsDate() {
        Booking booking = pending();
        booking.confirm(Instant.now());

        assertThrows(IllegalStateException.class,
                () -> booking.startTrip(DAY, booking.getStartCode(), Instant.now()));

        booking.assignDriver(UUID.randomUUID(), Instant.now());

        assertThrows(IllegalStateException.class,
                () -> booking.startTrip(DAY.minusDays(1), booking.getStartCode(), Instant.now()));

        assertThrows(IllegalArgumentException.class, () -> booking.startTrip(DAY, "wrong", Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> booking.startTrip(DAY, null, Instant.now()));

        booking.startTrip(DAY, booking.getStartCode(), Instant.now());
        assertEquals(BookingStatus.IN_PROGRESS, booking.getStatus());
    }

    @Test
    void onlyATripInProgressCanBeCompleted() {
        Booking booking = pending();
        booking.confirm(Instant.now());

        assertThrows(IllegalStateException.class, () -> booking.completeTrip(Instant.now()));

        booking.assignDriver(UUID.randomUUID(), Instant.now());
        booking.startTrip(DAY, booking.getStartCode(), Instant.now());
        assertNull(booking.codeToShowTourist());
        booking.completeTrip(Instant.now());

        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertFalse(booking.blocksVehicle(Instant.now()));
    }

    @Test
    void anIdempotencyKeyIsRequired() {
        assertThrows(IllegalArgumentException.class, () ->
                Booking.create(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                        TripType.CHAUFFEUR_ONE_WAY, DAY, DAY, 4, "A", "B", "INR",
                        BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, List.of(), Instant.now(), " "));
    }
}
