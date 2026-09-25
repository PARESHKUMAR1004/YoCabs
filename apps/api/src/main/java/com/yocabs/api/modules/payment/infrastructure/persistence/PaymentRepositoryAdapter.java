package com.yocabs.api.modules.payment.infrastructure.persistence;

import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import com.yocabs.api.modules.payment.domain.model.PaymentTransaction;
import com.yocabs.api.modules.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private static final Set<PaymentStatus> PAID =
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);

    private final PaymentJpaRepository payments;
    private final PaymentTransactionJpaRepository transactions;

    public PaymentRepositoryAdapter(
            PaymentJpaRepository payments,
            PaymentTransactionJpaRepository transactions
    ) {
        this.payments = payments;
        this.transactions = transactions;
    }

    @Override
    @Transactional
    public Payment create(Payment payment) {
        return payments.saveAndFlush(PaymentEntity.fromDomain(payment)).toDomain();
    }

    @Override
    @Transactional
    public Payment update(Payment payment) {
        PaymentEntity entity =
                payments.findById(payment.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Payment not found: " + payment.getId()));

        entity.updateFromDomain(payment);
        payments.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return payments.findById(id).map(PaymentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByGatewayOrderId(String gateway, String gatewayOrderId) {
        return payments.findByGatewayAndGatewayOrderId(gateway, gatewayOrderId)
                .map(PaymentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findInitiatedByBookingId(UUID bookingId) {
        return payments.findFirstByBookingIdAndStatus(bookingId, PaymentStatus.INITIATED)
                .map(PaymentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findPaidByBookingId(UUID bookingId) {
        return payments.findFirstByBookingIdAndStatusIn(bookingId, PAID)
                .map(PaymentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByBookingId(UUID bookingId) {
        return payments.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(PaymentEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public PaymentTransaction appendTransaction(PaymentTransaction transaction) {
        return transactions.saveAndFlush(PaymentTransactionEntity.fromDomain(transaction)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean transactionExistsForEvent(String gatewayEventId) {
        return transactions.existsByGatewayEventId(gatewayEventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> findTransactions(UUID paymentId) {
        return transactions.findByPaymentIdOrderByCreatedAtAsc(paymentId).stream()
                .map(PaymentTransactionEntity::toDomain).toList();
    }
}
