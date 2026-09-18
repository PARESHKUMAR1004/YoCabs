package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleCategoryEligibilityRule
        implements VehicleEligibilityRule {

    @Override
    public boolean isEligible(
            Vehicle vehicle,
            TripSearchCriteria criteria
    ) {

        if (criteria.vehicleCategory() == null) {
            return true;
        }

        return vehicle.getCategory()
                == criteria.vehicleCategory();
    }
}