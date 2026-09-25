package com.yocabs.api.modules.negotiation.infrastructure.persistence;

import com.yocabs.api.modules.negotiation.domain.model.Negotiation;
import com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus;
import com.yocabs.api.modules.negotiation.domain.repository.NegotiationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NegotiationRepositoryAdapter implements NegotiationRepository {

    private final NegotiationJpaRepository jpa;

    public NegotiationRepositoryAdapter(NegotiationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Negotiation create(Negotiation negotiation) {
        return jpa.saveAndFlush(NegotiationEntity.fromDomain(negotiation)).toDomain();
    }

    @Override
    @Transactional
    public Negotiation update(Negotiation negotiation) {
        NegotiationEntity entity =
                jpa.findById(negotiation.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Negotiation not found: " + negotiation.getId()));

        entity.updateFromDomain(negotiation);
        jpa.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Negotiation> findById(UUID id) {
        return jpa.findById(id).map(NegotiationEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Negotiation> findByTripRequestIdAndPartnerId(UUID tripRequestId, UUID travelPartnerId) {
        return jpa.findByTripRequestIdAndTravelPartnerId(tripRequestId, travelPartnerId)
                .map(NegotiationEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Negotiation> findByTripRequestId(UUID tripRequestId) {
        return jpa.findByTripRequestId(tripRequestId).stream()
                .map(NegotiationEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Negotiation> findByPartnerId(UUID travelPartnerId, NegotiationStatus status) {
        return (status == null
                ? jpa.findByTravelPartnerIdOrderByCreatedAtDesc(travelPartnerId)
                : jpa.findByTravelPartnerIdAndStatusOrderByCreatedAtDesc(travelPartnerId, status))
                .stream().map(NegotiationEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Negotiation> findOpenDueForExpiry(Instant now) {
        return jpa.findOpenDueForExpiry(now).stream()
                .map(NegotiationEntity::toDomain).toList();
    }
}
