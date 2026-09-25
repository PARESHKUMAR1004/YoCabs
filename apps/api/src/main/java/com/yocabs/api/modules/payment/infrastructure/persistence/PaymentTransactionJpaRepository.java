package com.yocabs.api.modules.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTransactionJpaRepository
        extends JpaRepository<PaymentTransactionEntity, UUID> {

    boolean existsByGatewayEventId(String gatewayEventId);

    List<PaymentTransactionEntity> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
