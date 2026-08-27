package com.yocabs.api.modules.pricing.domain;

import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;

public interface PricingStrategy {

    boolean supports(TripType tripType);

    PriceCalculation calculate(
            PricingContext context,
            Vehicle vehicle,
            PricingConfiguration configuration
    );
}