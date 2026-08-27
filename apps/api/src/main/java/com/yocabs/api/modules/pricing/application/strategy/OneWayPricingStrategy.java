package com.yocabs.api.modules.pricing.application.strategy;

import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.pricing.domain.PriceComponent;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.pricing.domain.PricingStrategy;
import com.yocabs.api.modules.pricing.domain.model.OneWayPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OneWayPricingStrategy
        implements PricingStrategy {

    private static final String CURRENCY = "INR";

    @Override
    public boolean supports(
            TripType tripType
    ) {

        return tripType
                == TripType.CHAUFFEUR_ONE_WAY;
    }

    @Override
    public PriceCalculation calculate(
            PricingContext context,
            Vehicle vehicle,
            PricingConfiguration configuration
    ) {

        OneWayPricingConfiguration pricing =
                requireConfiguration(configuration);

        if (context.distanceKm() == null) {
            throw new IllegalArgumentException(
                    "Distance is required for one-way pricing"
            );
        }

        BigDecimal billableDistance =
                context.distanceKm()
                        .max(
                                pricing.getMinimumBillableKm()
                        );

        BigDecimal distanceCharge =
                billableDistance.multiply(
                        pricing.getPerKmCharge()
                );

        BigDecimal total =
                pricing.getBaseFee()
                        .add(distanceCharge)
                        .add(pricing.getDriverAllowance());

        List<PriceComponent> components =
                new ArrayList<>();

        components.add(
                new PriceComponent(
                        "BASE_FEE",
                        "Base fee",
                        pricing.getBaseFee()
                )
        );

        components.add(
                new PriceComponent(
                        "DISTANCE_CHARGE",
                        "Distance charge",
                        distanceCharge
                )
        );

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

    private OneWayPricingConfiguration
    requireConfiguration(
            PricingConfiguration configuration
    ) {

        if (!(configuration
                instanceof OneWayPricingConfiguration pricing)) {

            throw new IllegalArgumentException(
                    "One-way pricing requires "
                            + "OneWayPricingConfiguration"
            );
        }

        return pricing;
    }
}