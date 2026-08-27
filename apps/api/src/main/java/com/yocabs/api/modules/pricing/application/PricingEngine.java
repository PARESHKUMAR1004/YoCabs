package com.yocabs.api.modules.pricing.application;

import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.pricing.domain.PricingStrategy;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PricingEngine {

    private final List<PricingStrategy> strategies;

    public PricingEngine(
            List<PricingStrategy> strategies
    ) {
        this.strategies = List.copyOf(strategies);
    }

    public PriceCalculation calculate(
            TripType tripType,
            PricingContext context,
            Vehicle vehicle,
            PricingConfiguration configuration
    ) {

        validateInput(
                tripType,
                context,
                vehicle,
                configuration
        );

        PricingStrategy strategy =
                findStrategy(tripType);

        return strategy.calculate(
                context,
                vehicle,
                configuration
        );
    }

    private void validateInput(
            TripType tripType,
            PricingContext context,
            Vehicle vehicle,
            PricingConfiguration configuration
    ) {

        if (tripType == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Pricing context is required"
            );
        }

        if (vehicle == null) {
            throw new IllegalArgumentException(
                    "Vehicle is required"
            );
        }

        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Pricing configuration is required"
            );
        }

        if (!configuration.isActive()) {
            throw new IllegalStateException(
                    "Pricing configuration is inactive"
            );
        }

        if (configuration.getVehicleId()
                .equals(vehicle.getId()) == false) {

            throw new IllegalArgumentException(
                    "Pricing configuration does not belong to vehicle"
            );
        }

        if (configuration.getTripType() != tripType) {
            throw new IllegalArgumentException(
                    "Pricing configuration does not match trip type"
            );
        }
    }

    private PricingStrategy findStrategy(
            TripType tripType
    ) {

        List<PricingStrategy> matchingStrategies =
                strategies.stream()
                        .filter(strategy ->
                                strategy.supports(tripType)
                        )
                        .toList();

        if (matchingStrategies.isEmpty()) {
            throw new IllegalStateException(
                    "No pricing strategy configured for trip type: "
                            + tripType
            );
        }

        if (matchingStrategies.size() > 1) {
            throw new IllegalStateException(
                    "Multiple pricing strategies configured for trip type: "
                            + tripType
            );
        }

        return matchingStrategies.getFirst();
    }
}