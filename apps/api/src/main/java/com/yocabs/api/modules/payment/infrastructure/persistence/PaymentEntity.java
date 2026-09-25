package com.yocabs.api.modules.payment.infrastructure.persistence;

import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "gateway", nullable = false, length = 30)
    private String gateway;

    @Column(name = "gateway_order_id", nullable = false, length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "refunded_amount", nullable = false)
    private BigDecimal refundedAmount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentEntity() {
        // JPA
    }

    static PaymentEntity fromDomain(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.getId();
        entity.bookingId = payment.getBookingId();
        entity.amount = payment.getAmount();
        entity.currency = payment.getCurrency();
        entity.gateway = payment.getGateway();
        entity.gatewayOrderId = payment.getGatewayOrderId();
        entity.createdAt = payment.getCreatedAt();
        entity.updateFromDomain(payment);
        return entity;
    }

    void updateFromDomain(Payment payment) {
        this.status = payment.getStatus();
        this.gatewayPaymentId = payment.getGatewayPaymentId();
        this.refundedAmount = payment.getRefundedAmount();
        this.updatedAt = payment.getUpdatedAt();
    }

    Payment toDomain() {
        return Payment.reconstitute(
                id, bookingId, amount, currency, status, gateway, gatewayOrderId,
                gatewayPaymentId, refundedAmount, version, createdAt, updatedAt
        );
    }
}
