package com.yocabs.api.modules.tripsearch.domain;

import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;

public record TripSearchCriteria(
        Itinerary itinerary,
        int passengerCount,
        VehicleCategory vehicleCategory,
        TripType tripType
) {

    public TripSearchCriteria {

        if (itinerary == null) {
            throw new IllegalArgumentException(
                    "Itinerary is required"
            );
        }

        if (passengerCount <= 0) {
            throw new IllegalArgumentException(
                    "Passenger count must be greater than zero"
            );
        }
    }


}