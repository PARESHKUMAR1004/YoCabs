package com.yocabs.api.modules.report.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read models for travel-partner reports. */
public final class PartnerReports {

    private PartnerReports() {
    }

    public record DateRange(LocalDate from, LocalDate to) {
    }

    public record EarningsReport(
            DateRange range,
            long completedTrips,
            long upcomingTrips,
            BigDecimal grossFare,
            BigDecimal commission,
            BigDecimal partnerEarnings,
            BigDecimal tokenCollectedByYoCabs,
            BigDecimal walletBalance,
            List<MonthlyEarnings> months
    ) {
    }

    public record MonthlyEarnings(String month, long completedTrips, BigDecimal grossFare, BigDecimal commission) {
    }

    public record TripsReport(DateRange range, Map<String, Long> countsByStatus, List<TripRow> trips) {
    }

    public record TripRow(
            UUID bookingId,
            LocalDate startDate,
            LocalDate endDate,
            String pickup,
            String destination,
            String status,
            BigDecimal totalAmount,
            String vehicle,
            String driver
    ) {
    }

    public record VehicleRow(
            UUID vehicleId,
            String registrationNumber,
            String makeModel,
            String category,
            String status,
            long completedTrips,
            long cancelledTrips,
            BigDecimal revenue,
            long daysOnRoad
    ) {
    }

    public record DriverRow(
            UUID driverId,
            String name,
            String status,
            long completedTrips,
            double averageRating,
            long reviewCount
    ) {
    }

    public record CancellationsReport(
            DateRange range,
            Map<String, Long> countsByCancelledBy,
            BigDecimal totalRefunded,
            List<CancellationRow> cancellations
    ) {
    }

    public record CancellationRow(
            UUID bookingId,
            LocalDate startDate,
            String pickup,
            String destination,
            BigDecimal totalAmount,
            String reason,
            String cancelledBy,
            Instant cancelledAt,
            BigDecimal refunded
    ) {
    }
}
