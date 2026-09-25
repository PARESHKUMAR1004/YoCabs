package com.yocabs.api.modules.negotiation.infrastructure.persistence;

import com.yocabs.api.modules.negotiation.domain.model.Negotiation;
import com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
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
@Table(name = "negotiations")
public class NegotiationEntity {

    @Id
    private UUID id;

    @Column(name = "trip_request_id", nullable = false)
    private UUID tripRequestId;

    @Column(name = "tourist_id", nullable = false)
    private UUID touristId;

    @Column(name = "travel_partner_id", nullable = false)
    private UUID travelPartnerId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 50)
    private TripType tripType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "listed_amount", nullable = false)
    private BigDecimal listedAmount;

    @Column(name = "offered_amount", nullable = false)
    private BigDecimal offeredAmount;

    @Column(name = "counter_amount")
    private BigDecimal counterAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NegotiationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NegotiationEntity() {
        // JPA
    }

    static NegotiationEntity fromDomain(Negotiation negotiation) {
        NegotiationEntity entity = new NegotiationEntity();
        entity.id = negotiation.getId();
        entity.tripRequestId = negotiation.getTripRequestId();
        entity.touristId = negotiation.getTouristId();
        entity.travelPartnerId = negotiation.getTravelPartnerId();
        entity.vehicleId = negotiation.getVehicleId();
        entity.tripType = negotiation.getTripType();
        entity.currency = negotiation.getCurrency();
        entity.listedAmount = negotiation.getListedAmount();
        entity.offeredAmount = negotiation.getOfferedAmount();
        entity.createdAt = negotiation.getCreatedAt();
        entity.updateFromDomain(negotiation);
        return entity;
    }

    void updateFromDomain(Negotiation negotiation) {
        this.counterAmount = negotiation.getCounterAmount();
        this.status = negotiation.getStatus();
        this.expiresAt = negotiation.getExpiresAt();
        this.consumed = negotiation.isConsumed();
        this.updatedAt = negotiation.getUpdatedAt();
    }

    Negotiation toDomain() {
        return Negotiation.reconstitute(
                id, tripRequestId, touristId, travelPartnerId, vehicleId, tripType, currency,
                listedAmount, offeredAmount, counterAmount, status, expiresAt, consumed,
                version, createdAt, updatedAt
        );
    }
}
