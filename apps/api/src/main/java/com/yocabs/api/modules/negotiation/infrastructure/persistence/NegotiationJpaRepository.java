package com.yocabs.api.modules.negotiation.infrastructure.persistence;

import com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NegotiationJpaRepository
        extends JpaRepository<NegotiationEntity, UUID> {

    Optional<NegotiationEntity> findByTripRequestIdAndTravelPartnerId(
            UUID tripRequestId,
            UUID travelPartnerId
    );

    List<NegotiationEntity> findByTripRequestId(UUID tripRequestId);

    List<NegotiationEntity> findByTravelPartnerIdOrderByCreatedAtDesc(UUID travelPartnerId);

    List<NegotiationEntity> findByTravelPartnerIdAndStatusOrderByCreatedAtDesc(
            UUID travelPartnerId,
            NegotiationStatus status
    );

    @Query("select n from NegotiationEntity n "
            + "where n.status in (com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus.OFFER_SENT, "
            + "com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus.COUNTER_SENT) "
            + "and n.expiresAt <= :now")
    List<NegotiationEntity> findOpenDueForExpiry(@Param("now") Instant now);
}
