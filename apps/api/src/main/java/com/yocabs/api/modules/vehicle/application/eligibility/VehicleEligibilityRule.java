package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;

public interface VehicleEligibilityRule {

    boolean isEligible(
            Vehicle vehicle,
            TripSearchCriteria criteria
    );
}