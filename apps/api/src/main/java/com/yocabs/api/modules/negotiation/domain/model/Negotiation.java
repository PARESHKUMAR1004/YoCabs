package com.yocabs.api.modules.negotiation.domain.model;

import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Private, single-round negotiation: the tourist makes one offer; the
 * partner accepts, rejects, or makes one counter; the tourist accepts or
 * rejects the counter. Nothing else.
 */
public class Negotiation {

    private final UUID id;
    private final UUID tripRequestId;
    private final UUID touristId;
    private final UUID travelPartnerId;
    private final UUID vehicleId;
    private final TripType tripType;
    private final String currency;
    private final BigDecimal listedAmount;
    private final BigDecimal offeredAmount;
    private BigDecimal counterAmount;
    private NegotiationStatus status;
    private Instant expiresAt;
    private boolean consumed;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Negotiation(
            UUID id,
            UUID tripRequestId,
            UUID touristId,
            UUID travelPartnerId,
            UUID vehicleId,
            TripType tripType,
            String currency,
            BigDecimal listedAmount,
            BigDecimal offeredAmount,
            BigDecimal counterAmount,
            NegotiationStatus status,
            Instant expiresAt,
            boolean consumed,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.tripRequestId = tripRequestId;
        this.touristId = touristId;
        this.travelPartnerId = travelPartnerId;
        this.vehicleId = vehicleId;
        this.tripType = tripType;
        this.currency = currency;
        this.listedAmount = listedAmount;
        this.offeredAmount = offeredAmount;
        this.counterAmount = counterAmount;
        this.status = status;
        this.expiresAt = expiresAt;
        this.consumed = consumed;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Negotiation start(
            UUID tripRequestId,
            UUID touristId,
            UUID travelPartnerId,
            UUID vehicleId,
            TripType tripType,
            String currency,
            BigDecimal listedAmount,
            BigDecimal offeredAmount,
            Duration validity
    ) {
        require(tripRequestId, "Trip request");
        require(touristId, "Tourist");
        require(travelPartnerId, "Travel partner");
        require(vehicleId, "Vehicle");
        require(tripType, "Trip type");

        if (offeredAmount == null || offeredAmount.signum() <= 0) {
            throw new IllegalArgumentException("Offer must be greater than zero");
        }

        if (listedAmount == null || offeredAmount.compareTo(listedAmount) >= 0) {
            throw new IllegalArgumentException(
                    "Offer must be lower than the listed price"
            );
        }

        Instant now = Instant.now();

        return new Negotiation(
                UUID.randomUUID(), tripRequestId, touristId, travelPartnerId, vehicleId,
                tripType, currency, listedAmount, offeredAmount, null,
                NegotiationStatus.OFFER_SENT, now.plus(validity), false, 0L, now, now
        );
    }

    public static Negotiation reconstitute(
            UUID id,
            UUID tripRequestId,
            UUID touristId,
            UUID travelPartnerId,
            UUID vehicleId,
            TripType tripType,
            String currency,
            BigDecimal listedAmount,
            BigDecimal offeredAmount,
            BigDecimal counterAmount,
            NegotiationStatus status,
            Instant expiresAt,
            boolean consumed,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Negotiation(
                id, tripRequestId, touristId, travelPartnerId, vehicleId, tripType, currency,
                listedAmount, offeredAmount, counterAmount, status, expiresAt, consumed,
                version, createdAt, updatedAt
        );
    }

    public void partnerAccepts(Instant now) {
        requireOpen(NegotiationStatus.OFFER_SENT, now);
        transition(NegotiationStatus.ACCEPTED, now);
    }

    public void partnerRejects(Instant now) {
        requireOpen(NegotiationStatus.OFFER_SENT, now);
        transition(NegotiationStatus.REJECTED, now);
    }

    public void partnerCounters(BigDecimal counter, Instant now, Duration validity) {
        requireOpen(NegotiationStatus.OFFER_SENT, now);

        if (counter == null
                || counter.compareTo(offeredAmount) <= 0
                || counter.compareTo(listedAmount) >= 0) {
            throw new IllegalArgumentException(
                    "Counter offer must be higher than the tourist's offer and lower than the listed price"
            );
        }

        counterAmount = counter;
        expiresAt = now.plus(validity);
        transition(NegotiationStatus.COUNTER_SENT, now);
    }

    public void touristAcceptsCounter(Instant now) {
        requireOpen(NegotiationStatus.COUNTER_SENT, now);
        transition(NegotiationStatus.COUNTER_ACCEPTED, now);
    }

    public void touristRejectsCounter(Instant now) {
        requireOpen(NegotiationStatus.COUNTER_SENT, now);
        transition(NegotiationStatus.COUNTER_REJECTED, now);
    }

    /** Returns true when the negotiation was open and has now expired. */
    public boolean expireIfDue(Instant now) {
        if (status.isOpen() && !expiresAt.isAfter(now)) {
            transition(NegotiationStatus.EXPIRED, now);
            return true;
        }
        return false;
    }

    public boolean isAgreed() {
        return status == NegotiationStatus.ACCEPTED
                || status == NegotiationStatus.COUNTER_ACCEPTED;
    }

    public BigDecimal agreedAmount() {
        if (status == NegotiationStatus.ACCEPTED) {
            return offeredAmount;
        }
        if (status == NegotiationStatus.COUNTER_ACCEPTED) {
            return counterAmount;
        }
        throw new IllegalStateException("Negotiation has no agreed price");
    }

    public void markConsumed(Instant now) {
        if (!isAgreed()) {
            throw new IllegalStateException("Only an agreed negotiation can be used for a booking");
        }
        if (consumed) {
            throw new IllegalStateException("This negotiated price has already been used");
        }
        consumed = true;
        updatedAt = now;
    }

    /** Status as the caller should see it, even if the expiry job has not yet run. */
    public NegotiationStatus effectiveStatus(Instant now) {
        return status.isOpen() && !expiresAt.isAfter(now)
                ? NegotiationStatus.EXPIRED
                : status;
    }

    private void requireOpen(NegotiationStatus expected, Instant now) {
        if (status != expected) {
            throw new IllegalStateException(
                    "This action is not allowed while the negotiation is " + status
            );
        }
        if (!expiresAt.isAfter(now)) {
            throw new IllegalStateException("The negotiation has expired");
        }
    }

    private void transition(NegotiationStatus next, Instant now) {
        status = next;
        updatedAt = now;
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    public UUID getId() { return id; }
    public UUID getTripRequestId() { return tripRequestId; }
    public UUID getTouristId() { return touristId; }
    public UUID getTravelPartnerId() { return travelPartnerId; }
    public UUID getVehicleId() { return vehicleId; }
    public TripType getTripType() { return tripType; }
    public String getCurrency() { return currency; }
    public BigDecimal getListedAmount() { return listedAmount; }
    public BigDecimal getOfferedAmount() { return offeredAmount; }
    public BigDecimal getCounterAmount() { return counterAmount; }
    public NegotiationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isConsumed() { return consumed; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
