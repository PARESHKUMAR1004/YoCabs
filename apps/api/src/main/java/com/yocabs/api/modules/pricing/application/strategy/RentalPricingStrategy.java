package com.yocabs.api.modules.pricing.application.strategy;

import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.pricing.domain.PriceComponent;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.pricing.domain.PricingStrategy;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RentalPricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class RentalPricingStrategy
        implements PricingStrategy {

    private static final String CURRENCY = "INR";

    @Override
    public boolean supports(
            TripType tripType
    ) {

        return tripType
                == TripType.CHAUFFEUR_RENTAL;
    }

    @Override
    public PriceCalculation calculate(
            PricingContext context,
            Vehicle vehicle,
            PricingConfiguration configuration
    ) {

        RentalPricingConfiguration pricing =
                requireConfiguration(configuration);

        validateContext(context);

        BigDecimal extraHours =
                calculateExtraHours(
                        context.duration(),
                        pricing.getIncludedDuration()
                );

        BigDecimal extraKm =
                calculateExtraKm(
                        context.distanceKm(),
                        pricing.getIncludedDistanceKm()
                );

        BigDecimal extraHourCharge =
                extraHours.multiply(
                        pricing.getExtraHourCharge()
                );

        BigDecimal extraKmCharge =
                extraKm.multiply(
                        pricing.getExtraKmCharge()
                );

        BigDecimal total =
                pricing.getPackagePrice()
                        .add(extraHourCharge)
                        .add(extraKmCharge)
                        .add(pricing.getDriverAllowance());

        List<PriceComponent> components =
                new ArrayList<>();

        components.add(
                new PriceComponent(
                        "PACKAGE_PRICE",
                        "Rental package price",
                        pricing.getPackagePrice()
                )
        );

        if (extraHours.signum() > 0) {

            components.add(
                    new PriceComponent(
                            "EXTRA_HOUR_CHARGE",
                            "Additional hour charges",
                            extraHourCharge
                    )
            );
        }

        if (extraKm.signum() > 0) {

            components.add(
                    new PriceComponent(
                            "EXTRA_KM_CHARGE",
                            "Additional kilometre charges",
                            extraKmCharge
                    )
            );
        }

        if (pricing.getDriverAllowance()
                .signum() > 0) {

            components.add(
                    new PriceComponent(
                            "DRIVER_ALLOWANCE",
                            "Driver allowance",
                            pricing.getDriverAllowance()
                    )
            );
        }

        return new PriceCalculation(
                total,
                CURRENCY,
                components
        );
    }

    private BigDecimal calculateExtraHours(
            Duration actualDuration,
            Duration includedDuration
    ) {

        Duration extraDuration =
                actualDuration.minus(includedDuration);

        if (extraDuration.isZero()
                || extraDuration.isNegative()) {

            return BigDecimal.ZERO;
        }

        long extraMinutes =
                extraDuration.toMinutes();

        /*
         * Any started hour is charged as one full hour.
         *
         * 8h 10m -> 1 extra hour
         * 8h 45m -> 1 extra hour
         * 9h 10m -> 2 extra hours
         */
        long extraHours =
                (long) Math.ceil(
                        extraMinutes / 60.0
                );

        return BigDecimal.valueOf(extraHours);
    }

    private BigDecimal calculateExtraKm(
            BigDecimal actualDistance,
            BigDecimal includedDistance
    ) {

        if (actualDistance == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal extraKm =
                actualDistance
                        .subtract(includedDistance);

        if (extraKm.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        /*
         * We charge every additional kilometre.
         *
         * If later the business requires rounding,
         * this can be changed inside this strategy.
         */
        return extraKm.setScale(
                2,
                RoundingMode.CEILING
        );
    }

    private void validateContext(
            PricingContext context
    ) {

        if (context.duration() == null) {
            throw new IllegalArgumentException(
                    "Duration is required for rental pricing"
            );
        }

        if (context.duration().isZero()
                || context.duration().isNegative()) {

            throw new IllegalArgumentException(
                    "Rental duration must be greater than zero"
            );
        }

        if (context.distanceKm() == null) {
            throw new IllegalArgumentException(
                    "Distance is required for rental pricing"
            );
        }
    }

    private RentalPricingConfiguration
    requireConfiguration(
            PricingConfiguration configuration
    ) {

        if (!(configuration
                instanceof RentalPricingConfiguration pricing)) {

            throw new IllegalArgumentException(
                    "Rental pricing requires "
                            + "RentalPricingConfiguration"
            );
        }

        return pricing;
    }
}