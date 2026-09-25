package com.yocabs.api.modules.payment.domain.repository;

import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.model.PaymentTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment create(Payment payment);

    Payment update(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByGatewayOrderId(String gateway, String gatewayOrderId);

    Optional<Payment> findInitiatedByBookingId(UUID bookingId);

    Optional<Payment> findPaidByBookingId(UUID bookingId);

    List<Payment> findByBookingId(UUID bookingId);

    PaymentTransaction appendTransaction(PaymentTransaction transaction);

    boolean transactionExistsForEvent(String gatewayEventId);

    List<PaymentTransaction> findTransactions(UUID paymentId);
}
