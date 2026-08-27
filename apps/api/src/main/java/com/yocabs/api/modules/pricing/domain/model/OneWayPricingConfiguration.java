package com.yocabs.api.modules.pricing.domain.model;

import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class OneWayPricingConfiguration
        extends PricingConfiguration {

    private BigDecimal baseFee;

    private BigDecimal perKmCharge;

    private BigDecimal driverAllowance;

    private BigDecimal minimumBillableKm;

    /*
     * Constructor used when creating a new configuration.
     */
    public OneWayPricingConfiguration(
            UUID id,
            UUID vehicleId,
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm
    ) {

        super(
                id,
                vehicleId,
                TripType.CHAUFFEUR_ONE_WAY
        );

        validate(
                baseFee,
                perKmCharge,
                driverAllowance,
                minimumBillableKm
        );

        this.baseFee = baseFee;
        this.perKmCharge = perKmCharge;
        this.driverAllowance = driverAllowance;
        this.minimumBillableKm = minimumBillableKm;
    }

    /*
     * Constructor used when reconstructing the domain
     * object from the persistence layer.
     */
    public OneWayPricingConfiguration(
            UUID id,
            UUID vehicleId,
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {

        super(
                id,
                vehicleId,
                TripType.CHAUFFEUR_ONE_WAY,
                active,
                createdAt,
                updatedAt
        );

        validate(
                baseFee,
                perKmCharge,
                driverAllowance,
                minimumBillableKm
        );

        this.baseFee = baseFee;
        this.perKmCharge = perKmCharge;
        this.driverAllowance = driverAllowance;
        this.minimumBillableKm = minimumBillableKm;
    }

    /*
     * Updates the partner-configured pricing values.
     */
    public void update(
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm
    ) {

        validate(
                baseFee,
                perKmCharge,
                driverAllowance,
                minimumBillableKm
        );

        this.baseFee = baseFee;
        this.perKmCharge = perKmCharge;
        this.driverAllowance = driverAllowance;
        this.minimumBillableKm = minimumBillableKm;

        markUpdated();
    }

    private static void validate(
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm
    ) {

        validateNonNegative(
                baseFee,
                "Base fee"
        );

        validateNonNegative(
                perKmCharge,
                "Per km charge"
        );

        validateNonNegative(
                driverAllowance,
                "Driver allowance"
        );

        validateNonNegative(
                minimumBillableKm,
                "Minimum billable km"
        );
    }

    private static void validateNonNegative(
            BigDecimal value,
            String field
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    field + " cannot be negative"
            );
        }
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public BigDecimal getPerKmCharge() {
        return perKmCharge;
    }

    public BigDecimal getDriverAllowance() {
        return driverAllowance;
    }

    public BigDecimal getMinimumBillableKm() {
        return minimumBillableKm;
    }
}