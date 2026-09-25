package com.yocabs.api.modules.negotiation.domain.repository;

import com.yocabs.api.modules.negotiation.domain.model.Negotiation;
import com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NegotiationRepository {

    Negotiation create(Negotiation negotiation);

    Negotiation update(Negotiation negotiation);

    Optional<Negotiation> findById(UUID id);

    Optional<Negotiation> findByTripRequestIdAndPartnerId(UUID tripRequestId, UUID travelPartnerId);

    List<Negotiation> findByTripRequestId(UUID tripRequestId);

    List<Negotiation> findByPartnerId(UUID travelPartnerId, NegotiationStatus status);

    List<Negotiation> findOpenDueForExpiry(Instant now);
}
