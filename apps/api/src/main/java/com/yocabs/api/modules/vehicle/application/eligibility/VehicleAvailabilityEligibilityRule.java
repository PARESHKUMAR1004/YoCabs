package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import org.springframework.stereotype.Component;

@Component
public class VehicleAvailabilityEligibilityRule
        implements VehicleEligibilityRule {

    @Override
    public boolean isEligible(
            Vehicle vehicle,
            TripSearchCriteria criteria
    ) {

        return vehicle.getStatus()
                == VehicleStatus.AVAILABLE;
    }
}