package com.yocabs.api.modules.vehicle.application.eligibility;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.PassengerCount;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripBrief;
import com.yocabs.api.modules.triprequest.domain.valueobject.TouristId;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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

        TripRequest tripRequest =
                createTripRequest(null);

        assertTrue(
                rule.isEligible(
                        vehicle,
                        tripRequest
                )
        );
    }

    @Test
    void shouldAcceptVehicleWhenCategoryMatches() {

        Vehicle vehicle =
                createVehicle(
                        VehicleCategory.MUV
                );

        TripRequest tripRequest =
                createTripRequest(
                        VehicleCategory.MUV
                );

        assertTrue(
                rule.isEligible(
                        vehicle,
                        tripRequest
                )
        );
    }

    @Test
    void shouldRejectVehicleWhenCategoryDoesNotMatch() {

        Vehicle vehicle =
                createVehicle(
                        VehicleCategory.SUV
                );

        TripRequest tripRequest =
                createTripRequest(
                        VehicleCategory.MUV
                );

        assertFalse(
                rule.isEligible(
                        vehicle,
                        tripRequest
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

    private TripRequest createTripRequest(
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

        return TripRequest.create(
                new TouristId(
                        UUID.randomUUID()
                ),
                itinerary,
                new TravelDateRange(
                        LocalDate.of(2026, 8, 30),
                        LocalDate.of(2026, 8, 30)
                ),
                new PassengerCount(4),
                new TripBrief(
                        "Bhubaneswar to Puri"
                ),
                TripType.CHAUFFEUR_ONE_WAY,
                category
        );
    }
}