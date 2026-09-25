package com.yocabs.api.modules.payment.infrastructure.persistence;

import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository
        extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByGatewayAndGatewayOrderId(String gateway, String gatewayOrderId);

    Optional<PaymentEntity> findFirstByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    Optional<PaymentEntity> findFirstByBookingIdAndStatusIn(
            UUID bookingId,
            Collection<PaymentStatus> statuses
    );

    List<PaymentEntity> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);
}
