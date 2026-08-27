package com.yocabs.api.modules.travelpartner.application.model;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;

import java.util.List;

public record EligibleTravelPartner(
        TravelPartner travelPartner,
        List<Vehicle> eligibleVehicles
) {

    public EligibleTravelPartner {

        if (travelPartner == null) {
            throw new IllegalArgumentException(
                    "Travel partner is required"
            );
        }

        if (eligibleVehicles == null) {
            throw new IllegalArgumentException(
                    "Eligible vehicles are required"
            );
        }

        eligibleVehicles =
                List.copyOf(eligibleVehicles);
    }
}