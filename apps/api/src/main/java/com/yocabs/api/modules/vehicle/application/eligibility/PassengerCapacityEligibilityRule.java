package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class PassengerCapacityEligibilityRule
        implements VehicleEligibilityRule {

    @Override
    public boolean isEligible(
            Vehicle vehicle,
            TripRequest tripRequest
    ) {

        return vehicle.getPassengerCapacity()
                >= tripRequest
                .getPassengerCount()
                .value();
    }
}