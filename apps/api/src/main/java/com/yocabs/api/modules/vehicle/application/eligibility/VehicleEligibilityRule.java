package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;

public interface VehicleEligibilityRule {

    boolean isEligible(
            Vehicle vehicle,
            TripRequest tripRequest
    );
}