package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Component;

/**
 * A vehicle only appears in a search when the pickup falls inside one of its own service areas.
 * A vehicle with no area configured is never offered, so a partner opts each car in deliberately.
 */
@Component
public class VehicleServiceAreaRule
        implements VehicleEligibilityRule {

    @Override
    public boolean isEligible(
            Vehicle vehicle,
            TripSearchCriteria criteria
    ) {

        Location pickup =
                criteria.itinerary().pickup();

        return vehicle.coversLocation(
                pickup.latitude(),
                pickup.longitude()
        );
    }
}
