package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleCategoryEligibilityRuleTest {

    private final VehicleCategoryEligibilityRule rule =
            new VehicleCategoryEligibilityRule();

    @Test
    void shouldAcceptVehicleWhenNoCategoryIsRequested() {

        Vehicle vehicle =
                createVehicle(
                        VehicleCategory.MUV
                );

        TripSearchCriteria criteria =
                createCriteria(null);

        assertTrue(
                rule.isEligible(
                        vehicle,
                        criteria
                )
        );
    }

    @Test
    void shouldAcceptVehicleWhenCategoryMatches() {

        Vehicle vehicle =
                createVehicle(
                        VehicleCategory.MUV
                );

        TripSearchCriteria criteria =
                createCriteria(
                        VehicleCategory.MUV
                );

        assertTrue(
                rule.isEligible(
                        vehicle,
                        criteria
                )
        );
    }

    @Test
    void shouldRejectVehicleWhenCategoryDoesNotMatch() {

        Vehicle vehicle =
                createVehicle(
                        VehicleCategory.SUV
                );

        TripSearchCriteria criteria =
                createCriteria(
                        VehicleCategory.MUV
                );

        assertFalse(
                rule.isEligible(
                        vehicle,
                        criteria
                )
        );
    }

    private Vehicle createVehicle(
            VehicleCategory category
    ) {

        return Vehicle.create(
                UUID.randomUUID(),
                "OD02AB1234",
                "Toyota",
                "Innova",
                category,
                7
        );
    }

    private TripSearchCriteria createCriteria(
            VehicleCategory category
    ) {

        Location pickup =
                new Location(
                        "Bhubaneswar",
                        20.2961,
                        85.8245
                );

        Location destination =
                new Location(
                        "Puri",
                        19.8135,
                        85.8312
                );

        Itinerary itinerary =
                new Itinerary(
                        pickup,
                        List.of(),
                        destination
                );

        return new TripSearchCriteria(
                itinerary,
                4,
                category,
                TripType.CHAUFFEUR_ONE_WAY
        );
    }
}
