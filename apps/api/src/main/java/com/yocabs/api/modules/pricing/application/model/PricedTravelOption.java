package com.yocabs.api.modules.pricing.application.model;

import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;

public record PricedTravelOption(
        TravelPartner travelPartner,
        Vehicle vehicle,
        TripType tripType,
        PriceCalculation price
) {

    public PricedTravelOption {

        if (travelPartner == null) {
            throw new IllegalArgumentException(
                    "Travel partner is required"
            );
        }

        if (vehicle == null) {
            throw new IllegalArgumentException(
                    "Vehicle is required"
            );
        }

        if (tripType == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
            );
        }

        if (price == null) {
            throw new IllegalArgumentException(
                    "Price calculation is required"
            );

        }

        if (!vehicle.getTravelPartnerId()
                .equals(travelPartner.getId())) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to travel partner"
            );
        }
    }
}