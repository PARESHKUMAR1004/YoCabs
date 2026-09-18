package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.eligibility.ActiveTravelPartnerRule;
import com.yocabs.api.modules.travelpartner.application.eligibility.ServiceAreaEligibilityRule;
import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.PassengerCount;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripBrief;
import com.yocabs.api.modules.triprequest.domain.valueobject.TouristId;
import com.yocabs.api.modules.vehicle.application.eligibility.PassengerCapacityEligibilityRule;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleAvailabilityEligibilityRule;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleCategoryEligibilityRule;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleEligibilityRule;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripEligibilityServiceTest {

    @Test
    void shouldReturnOnlyVehiclesMatchingAllEligibilityRules() {

        TravelPartner partner =
                TravelPartner.create(
                        "ABC Travels"
                );

        partner.activate();

        Vehicle matchingVehicle =
                createVehicle(
                        partner.getId(),
                        "OD02AA1111",
                        VehicleCategory.MUV,
                        7,
                        true
                );

        Vehicle wrongCategoryVehicle =
                createVehicle(
                        partner.getId(),
                        "OD02BB2222",
                        VehicleCategory.SUV,
                        7,
                        true
                );

        Vehicle insufficientCapacityVehicle =
                createVehicle(
                        partner.getId(),
                        "OD02CC3333",
                        VehicleCategory.MUV,
                        4,
                        true
                );

        Vehicle unavailableVehicle =
                createVehicle(
                        partner.getId(),
                        "OD02DD4444",
                        VehicleCategory.MUV,
                        7,
                        false
                );

        TripRequest tripRequest =
                createTripRequest(
                        6,
                        VehicleCategory.MUV
                );

        VehicleRepository vehicleRepository =
                new InMemoryVehicleRepository(
                        List.of(
                                matchingVehicle,
                                wrongCategoryVehicle,
                                insufficientCapacityVehicle,
                                unavailableVehicle
                        )
                );

        TravelPartnerEligibilityService
                travelPartnerEligibilityService =
                (
                        request,
                        candidates
                ) -> candidates;

        List<VehicleEligibilityRule>
                vehicleEligibilityRules =
                List.of(
                        new VehicleAvailabilityEligibilityRule(),
                        new PassengerCapacityEligibilityRule(),
                        new VehicleCategoryEligibilityRule()
                );

        TripEligibilityService service =
                new TripEligibilityService(
                        travelPartnerEligibilityService,
                        vehicleRepository,
                        vehicleEligibilityRules
                );

        List<EligibleTravelPartner> results =
                service.findEligibleOptions(
                        tripRequest,
                        List.of(partner)
                );

        assertEquals(
                1,
                results.size()
        );

        assertEquals(
                1,
                results.getFirst()
                        .eligibleVehicles()
                        .size()
        );

        assertEquals(
                matchingVehicle.getId(),
                results.getFirst()
                        .eligibleVehicles()
                        .getFirst()
                        .getId()
        );
    }

    @Test
    void shouldAllowAnyVehicleCategoryWhenNoCategoryIsRequested() {

        TravelPartner partner =
                TravelPartner.create(
                        "ABC Travels"
                );

        partner.activate();

        Vehicle sedan =
                createVehicle(
                        partner.getId(),
                        "OD02AA1111",
                        VehicleCategory.SEDAN,
                        7,
                        true
                );

        Vehicle suv =
                createVehicle(
                        partner.getId(),
                        "OD02BB2222",
                        VehicleCategory.SUV,
                        7,
                        true
                );

        Vehicle muv =
                createVehicle(
                        partner.getId(),
                        "OD02CC3333",
                        VehicleCategory.MUV,
                        7,
                        true
                );

        TripRequest tripRequest =
                createTripRequest(
                        6,
                        null
                );

        VehicleRepository vehicleRepository =
                new InMemoryVehicleRepository(
                        List.of(
                                sedan,
                                suv,
                                muv
                        )
                );

        TravelPartnerEligibilityService
                travelPartnerEligibilityService =
                (
                        request,
                        candidates
                ) -> candidates;

        List<VehicleEligibilityRule>
                vehicleEligibilityRules =
                List.of(
                        new VehicleAvailabilityEligibilityRule(),
                        new PassengerCapacityEligibilityRule(),
                        new VehicleCategoryEligibilityRule()
                );

        TripEligibilityService service =
                new TripEligibilityService(
                        travelPartnerEligibilityService,
                        vehicleRepository,
                        vehicleEligibilityRules
                );

        List<EligibleTravelPartner> results =
                service.findEligibleOptions(
                        tripRequest,
                        List.of(partner)
                );

        assertEquals(
                1,
                results.size()
        );

        assertEquals(
                3,
                results.getFirst()
                        .eligibleVehicles()
                        .size()
        );
    }

    private Vehicle createVehicle(
            UUID travelPartnerId,
            String registrationNumber,
            VehicleCategory category,
            int capacity,
            boolean available
    ) {

        Vehicle vehicle =
                Vehicle.create(
                        travelPartnerId,
                        registrationNumber,
                        "Toyota",
                        "Innova",
                        category,
                        capacity
                );

        if (available) {
            vehicle.makeAvailable();
        }

        return vehicle;
    }

    private TripRequest createTripRequest(
            int passengerCount,
            VehicleCategory vehicleCategory
    ) {

        Location pickup =
                new Location(
                        "Bhubaneswar Airport",
                        20.2444,
                        85.8178
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
                        LocalDate.of(
                                2026,
                                8,
                                30
                        ),
                        LocalDate.of(
                                2026,
                                8,
                                30
                        )
                ),
                new PassengerCount(
                        passengerCount
                ),
                new TripBrief(
                        "Bhubaneswar to Puri"
                ),
                TripType.CHAUFFEUR_ONE_WAY,
                vehicleCategory
        );
    }

    private static class InMemoryVehicleRepository
            implements VehicleRepository {

        private final List<Vehicle> vehicles;

        private InMemoryVehicleRepository(
                List<Vehicle> vehicles
        ) {
            this.vehicles =
                    List.copyOf(vehicles);
        }

        @Override
        public Vehicle create(
                Vehicle vehicle
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vehicle update(
                Vehicle vehicle
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Vehicle> findById(
                UUID id
        ) {
            return vehicles.stream()
                    .filter(vehicle ->
                            vehicle.getId()
                                    .equals(id)
                    )
                    .findFirst();
        }

        @Override
        public List<Vehicle>
        findByTravelPartnerId(
                UUID travelPartnerId
        ) {
            return vehicles.stream()
                    .filter(vehicle ->
                            vehicle.getTravelPartnerId()
                                    .equals(
                                            travelPartnerId
                                    )
                    )
                    .toList();
        }

        @Override
        public List<Vehicle>
        findAvailableVehicles() {
            return vehicles.stream()
                    .filter(vehicle ->
                            vehicle.getStatus()
                                    .name()
                                    .equals(
                                            "AVAILABLE"
                                    )
                    )
                    .toList();
        }
    }
}