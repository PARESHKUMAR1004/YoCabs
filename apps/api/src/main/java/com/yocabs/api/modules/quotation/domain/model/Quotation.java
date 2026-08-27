package com.yocabs.api.modules.quotation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Quotation {

    private final UUID id;

    private final UUID tripRequestId;

    private final UUID travelPartnerId;

    private final BigDecimal amount;

    private final String currency;

    private final Instant validUntil;

    private QuotationStatus status;

    private final Instant createdAt;

    private Instant updatedAt;

    private Quotation(
            UUID id,
            UUID tripRequestId,
            UUID travelPartnerId,
            BigDecimal amount,
            String currency,
            Instant validUntil,
            QuotationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.tripRequestId = tripRequestId;
        this.travelPartnerId = travelPartnerId;
        this.amount = amount;
        this.currency = currency;
        this.validUntil = validUntil;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Quotation create(
            UUID tripRequestId,
            UUID travelPartnerId,
            BigDecimal amount,
            String currency,
            Instant validUntil
    ) {

        validateTripRequestId(tripRequestId);
        validateTravelPartnerId(travelPartnerId);
        validateAmount(amount);
        validateCurrency(currency);
        validateValidity(validUntil);

        Instant now = Instant.now();

        return new Quotation(
                UUID.randomUUID(),
                tripRequestId,
                travelPartnerId,
                amount,
                currency.trim().toUpperCase(),
                validUntil,
                QuotationStatus.SUBMITTED,
                now,
                now
        );
    }

    public void accept() {

        if (status != QuotationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a submitted quotation can be accepted"
            );
        }

        if (isExpired()) {
            throw new IllegalStateException(
                    "An expired quotation cannot be accepted"
            );
        }

        status = QuotationStatus.ACCEPTED;
        updatedAt = Instant.now();
    }

    public void reject() {

        if (status != QuotationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a submitted quotation can be rejected"
            );
        }

        status = QuotationStatus.REJECTED;
        updatedAt = Instant.now();
    }

    public void withdraw() {

        if (status != QuotationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a submitted quotation can be withdrawn"
            );
        }

        status = QuotationStatus.WITHDRAWN;
        updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(validUntil);
    }

    private static void validateTripRequestId(
            UUID tripRequestId
    ) {
        if (tripRequestId == null) {
            throw new IllegalArgumentException(
                    "Trip request ID is required"
            );
        }
    }

    private static void validateTravelPartnerId(
            UUID travelPartnerId
    ) {
        if (travelPartnerId == null) {
            throw new IllegalArgumentException(
                    "Travel partner ID is required"
            );
        }
    }

    private static void validateAmount(
            BigDecimal amount
    ) {
        if (amount == null) {
            throw new IllegalArgumentException(
                    "Quotation amount is required"
            );
        }

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Quotation amount must be greater than zero"
            );
        }
    }

    private static void validateCurrency(
            String currency
    ) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        if (currency.trim().length() != 3) {
            throw new IllegalArgumentException(
                    "Currency must be a 3-letter currency code"
            );
        }
    }

    private static void validateValidity(
            Instant validUntil
    ) {
        if (validUntil == null) {
            throw new IllegalArgumentException(
                    "Quotation validity is required"
            );
        }

        if (!validUntil.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Quotation must be valid in the future"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTripRequestId() {
        return tripRequestId;
    }

    public UUID getTravelPartnerId() {
        return travelPartnerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}