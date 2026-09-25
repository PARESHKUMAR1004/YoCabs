package com.yocabs.api.modules.payment.infrastructure.persistence;

import com.yocabs.api.modules.payment.domain.model.PaymentTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private PaymentTransaction.Type type;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, updatable = false)
    private PaymentTransaction.Outcome status;

    @Column(name = "gateway_event_id", length = 100, updatable = false)
    private String gatewayEventId;

    @Column(name = "gateway_reference", length = 100, updatable = false)
    private String gatewayReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentTransactionEntity() {
        // JPA
    }

    static PaymentTransactionEntity fromDomain(PaymentTransaction transaction) {
        PaymentTransactionEntity entity = new PaymentTransactionEntity();
        entity.id = transaction.id();
        entity.paymentId = transaction.paymentId();
        entity.type = transaction.type();
        entity.amount = transaction.amount();
        entity.status = transaction.status();
        entity.gatewayEventId = transaction.gatewayEventId();
        entity.gatewayReference = transaction.gatewayReference();
        entity.createdAt = transaction.createdAt();
        return entity;
    }

    PaymentTransaction toDomain() {
        return new PaymentTransaction(
                id, paymentId, type, amount, status, gatewayEventId, gatewayReference, createdAt
        );
    }
}
