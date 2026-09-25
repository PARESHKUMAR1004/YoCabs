package com.yocabs.api.modules.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID bookingId;
    private final PaymentPurpose purpose;
    private final BigDecimal amount;
    private final String currency;
    private PaymentStatus status;
    private final String gateway;
    private final String gatewayOrderId;
    private String gatewayPaymentId;
    private BigDecimal refundedAmount;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(
            UUID id,
            UUID bookingId,
            PaymentPurpose purpose,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String gateway,
            String gatewayOrderId,
            String gatewayPaymentId,
            BigDecimal refundedAmount,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.bookingId = bookingId;
        this.purpose = purpose;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.gateway = gateway;
        this.gatewayOrderId = gatewayOrderId;
        this.gatewayPaymentId = gatewayPaymentId;
        this.refundedAmount = refundedAmount;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment initiate(
            UUID bookingId,
            PaymentPurpose purpose,
            BigDecimal amount,
            String currency,
            String gateway,
            String gatewayOrderId
    ) {
        if (bookingId == null || purpose == null) {
            throw new IllegalArgumentException("Booking and purpose are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            throw new IllegalArgumentException("Gateway order is required");
        }

        Instant now = Instant.now();

        return new Payment(
                UUID.randomUUID(), bookingId, purpose, amount, currency, PaymentStatus.INITIATED,
                gateway, gatewayOrderId, null, BigDecimal.ZERO, 0L, now, now
        );
    }

    public static Payment reconstitute(
            UUID id,
            UUID bookingId,
            PaymentPurpose purpose,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String gateway,
            String gatewayOrderId,
            String gatewayPaymentId,
            BigDecimal refundedAmount,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Payment(
                id, bookingId, purpose, amount, currency, status, gateway, gatewayOrderId,
                gatewayPaymentId, refundedAmount, version, createdAt, updatedAt
        );
    }

    public void markSucceeded(String paymentReference) {
        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Only an initiated payment can succeed");
        }
        status = PaymentStatus.SUCCEEDED;
        gatewayPaymentId = paymentReference;
        updatedAt = Instant.now();
    }

    public void markFailed() {
        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Only an initiated payment can fail");
        }
        status = PaymentStatus.FAILED;
        updatedAt = Instant.now();
    }

    public BigDecimal refundableAmount() {
        return amount.subtract(refundedAmount);
    }

    public boolean isPaid() {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.PARTIALLY_REFUNDED
                || status == PaymentStatus.REFUNDED;
    }

    public void refund(BigDecimal refundAmount) {
        if (status != PaymentStatus.SUCCEEDED && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("This payment cannot be refunded");
        }
        if (refundAmount == null
                || refundAmount.signum() <= 0
                || refundAmount.compareTo(refundableAmount()) > 0) {
            throw new IllegalArgumentException("Invalid refund amount");
        }

        refundedAmount = refundedAmount.add(refundAmount);
        status = refundedAmount.compareTo(amount) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public PaymentPurpose getPurpose() { return purpose; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getGateway() { return gateway; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
