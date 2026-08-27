package com.yocabs.api.modules.pricing.interfaces.rest.dto;

import com.yocabs.api.modules.pricing.domain.model.OneWayPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RentalPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RoundTripPricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record PricingConfigurationResponse(

        UUID id,

        UUID vehicleId,

        TripType tripType,

        boolean active,

        BigDecimal baseFee,

        BigDecimal perKmCharge,

        BigDecimal driverAllowance,

        BigDecimal minimumBillableKm,

        Integer includedDurationMinutes,

        BigDecimal includedDistanceKm,

        BigDecimal packagePrice,

        BigDecimal extraHourCharge,

        BigDecimal extraKmCharge,

        Instant createdAt,

        Instant updatedAt
) {

    public static PricingConfigurationResponse
    fromDomain(
            PricingConfiguration configuration
    ) {

        BigDecimal baseFee = null;
        BigDecimal perKmCharge = null;
        BigDecimal driverAllowance = null;
        BigDecimal minimumBillableKm = null;

        Integer includedDurationMinutes = null;
        BigDecimal includedDistanceKm = null;
        BigDecimal packagePrice = null;
        BigDecimal extraHourCharge = null;
        BigDecimal extraKmCharge = null;

        if (configuration
                instanceof OneWayPricingConfiguration pricing) {

            baseFee = pricing.getBaseFee();
            perKmCharge = pricing.getPerKmCharge();
            driverAllowance =
                    pricing.getDriverAllowance();
            minimumBillableKm =
                    pricing.getMinimumBillableKm();

        } else if (configuration
                instanceof RoundTripPricingConfiguration pricing) {

            baseFee = pricing.getBaseFee();
            perKmCharge = pricing.getPerKmCharge();
            driverAllowance =
                    pricing.getDriverAllowance();
            minimumBillableKm =
                    pricing.getMinimumBillableKm();

        } else if (configuration
                instanceof RentalPricingConfiguration pricing) {

            Duration duration =
                    pricing.getIncludedDuration();

            includedDurationMinutes =
                    Math.toIntExact(
                            duration.toMinutes()
                    );

            includedDistanceKm =
                    pricing.getIncludedDistanceKm();

            packagePrice =
                    pricing.getPackagePrice();

            extraHourCharge =
                    pricing.getExtraHourCharge();

            extraKmCharge =
                    pricing.getExtraKmCharge();

            driverAllowance =
                    pricing.getDriverAllowance();
        }

        return new PricingConfigurationResponse(
                configuration.getId(),
                configuration.getVehicleId(),
                configuration.getTripType(),
                configuration.isActive(),
                baseFee,
                perKmCharge,
                driverAllowance,
                minimumBillableKm,
                includedDurationMinutes,
                includedDistanceKm,
                packagePrice,
                extraHourCharge,
                extraKmCharge,
                configuration.getCreatedAt(),
                configuration.getUpdatedAt()
        );
    }
}