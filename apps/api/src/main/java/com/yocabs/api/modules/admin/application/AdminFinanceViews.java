package com.yocabs.api.modules.admin.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read models for the admin finance screens. */
public final class AdminFinanceViews {

    private AdminFinanceViews() {
    }

    public record DateRange(LocalDate from, LocalDate to) {
    }

    public record PaymentsView(DateRange range, PaymentsSummary summary, List<PaymentRow> payments) {
    }

    public record PaymentsSummary(long paidPayments, BigDecimal collected, BigDecimal refunded, BigDecimal netCollected) {
    }

    public record PaymentRow(
            UUID paymentId,
            UUID bookingId,
            UUID travelPartnerId,
            UUID touristId,
            BigDecimal amount,
            String currency,
            String status,
            BigDecimal refundedAmount,
            String gateway,
            Instant createdAt
    ) {
    }

    public record RefundsView(DateRange range, BigDecimal totalRefunded, List<RefundRow> refunds) {
    }

    public record RefundRow(
            UUID transactionId,
            UUID paymentId,
            UUID bookingId,
            UUID travelPartnerId,
            BigDecimal amount,
            String cancellationReason,
            String cancelledBy,
            Instant refundedAt
    ) {
    }

    public record CancellationsView(
            DateRange range,
            Map<String, Long> countsByCancelledBy,
            BigDecimal totalRefunded,
            List<CancellationRow> cancellations
    ) {
    }

    public record CancellationRow(
            UUID bookingId,
            UUID travelPartnerId,
            UUID touristId,
            LocalDate startDate,
            BigDecimal totalAmount,
            BigDecimal tokenAmount,
            String reason,
            String cancelledBy,
            Instant cancelledAt,
            BigDecimal refunded
    ) {
    }
}
