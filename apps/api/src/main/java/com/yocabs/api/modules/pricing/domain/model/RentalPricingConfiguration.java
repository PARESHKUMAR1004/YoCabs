package com.yocabs.api.modules.pricing.domain.model;

import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RentalPricingConfiguration
        extends PricingConfiguration {

    private Duration includedDuration;

    private BigDecimal includedDistanceKm;

    private BigDecimal packagePrice;

    private BigDecimal extraHourCharge;

    private BigDecimal extraKmCharge;

    private BigDecimal driverAllowance;

    /*
     * Constructor used when creating a new configuration.
     */
    public RentalPricingConfiguration(
            UUID id,
            UUID vehicleId,
            Duration includedDuration,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge,
            BigDecimal driverAllowance
    ) {

        super(
                id,
                vehicleId,
                TripType.CHAUFFEUR_RENTAL
        );

        validate(
                includedDuration,
                includedDistanceKm,
                packagePrice,
                extraHourCharge,
                extraKmCharge,
                driverAllowance
        );

        this.includedDuration = includedDuration;
        this.includedDistanceKm = includedDistanceKm;
        this.packagePrice = packagePrice;
        this.extraHourCharge = extraHourCharge;
        this.extraKmCharge = extraKmCharge;
        this.driverAllowance = driverAllowance;
    }

    /*
     * Constructor used when reconstructing the domain
     * object from the persistence layer.
     */
    public RentalPricingConfiguration(
            UUID id,
            UUID vehicleId,
            Duration includedDuration,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge,
            BigDecimal driverAllowance,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {

        super(
                id,
                vehicleId,
                TripType.CHAUFFEUR_RENTAL,
                active,
                createdAt,
                updatedAt
        );

        validate(
                includedDuration,
                includedDistanceKm,
                packagePrice,
                extraHourCharge,
                extraKmCharge,
                driverAllowance
        );

        this.includedDuration = includedDuration;
        this.includedDistanceKm = includedDistanceKm;
        this.packagePrice = packagePrice;
        this.extraHourCharge = extraHourCharge;
        this.extraKmCharge = extraKmCharge;
        this.driverAllowance = driverAllowance;
    }

    /*
     * Updates the partner-configured rental pricing.
     */
    public void update(
            Duration includedDuration,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge,
            BigDecimal driverAllowance
    ) {

        validate(
                includedDuration,
                includedDistanceKm,
                packagePrice,
                extraHourCharge,
                extraKmCharge,
                driverAllowance
        );

        this.includedDuration = includedDuration;
        this.includedDistanceKm = includedDistanceKm;
        this.packagePrice = packagePrice;
        this.extraHourCharge = extraHourCharge;
        this.extraKmCharge = extraKmCharge;
        this.driverAllowance = driverAllowance;

        markUpdated();
    }

    private static void validate(
            Duration includedDuration,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge,
            BigDecimal driverAllowance
    ) {

        validateDuration(
                includedDuration
        );

        validateNonNegative(
                includedDistanceKm,
                "Included distance"
        );

        validateNonNegative(
                packagePrice,
                "Package price"
        );

        validateNonNegative(
                extraHourCharge,
                "Extra hour charge"
        );

        validateNonNegative(
                extraKmCharge,
                "Extra km charge"
        );

        validateNonNegative(
                driverAllowance,
                "Driver allowance"
        );
    }

    private static void validateDuration(
            Duration duration
    ) {

        if (duration == null) {
            throw new IllegalArgumentException(
                    "Included duration is required"
            );
        }

        if (duration.isZero()
                || duration.isNegative()) {

            throw new IllegalArgumentException(
                    "Included duration must be greater than zero"
            );
        }
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

    public Duration getIncludedDuration() {
        return includedDuration;
    }

    public BigDecimal getIncludedDistanceKm() {
        return includedDistanceKm;
    }

    public BigDecimal getPackagePrice() {
        return packagePrice;
    }

    public BigDecimal getExtraHourCharge() {
        return extraHourCharge;
    }

    public BigDecimal getExtraKmCharge() {
        return extraKmCharge;
    }

    public BigDecimal getDriverAllowance() {
        return driverAllowance;
    }
}