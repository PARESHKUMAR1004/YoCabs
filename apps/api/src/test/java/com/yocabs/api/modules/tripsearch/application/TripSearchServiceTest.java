package com.yocabs.api.modules.tripsearch.application;

import com.yocabs.api.modules.pricing.application.PricingEngine;
import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.application.service.TripPricingService;
import com.yocabs.api.modules.pricing.application.strategy.OneWayPricingStrategy;
import com.yocabs.api.modules.pricing.application.strategy.RentalPricingStrategy;
import com.yocabs.api.modules.pricing.application.strategy.RoundTripPricingStrategy;
import com.yocabs.api.modules.pricing.domain.model.OneWayPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RentalPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RoundTripPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.travelpartner.application.eligibility.ActiveTravelPartnerRule;
import com.yocabs.api.modules.travelpartner.application.service.DefaultTravelPartnerEligibilityService;
import com.yocabs.api.modules.travelpartner.application.service.TripEligibilityService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.application.eligibility.PassengerCapacityEligibilityRule;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleAvailabilityEligibilityRule;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleCategoryEligibilityRule;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleServiceAreaRule;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripSearchServiceTest {

    private static final Location AIRPORT =
            new Location("Bhubaneswar Airport", 20.2444, 85.8178);

    private static final Location PURI =
            new Location("Puri", 19.8135, 85.8312);

    private InMemoryTravelPartnerRepository partnerRepository;
    private InMemoryVehicleRepository vehicleRepository;
    private InMemoryPricingConfigurationRepository pricingRepository;
    private RecordingRouteCalculationService routeService;
    private TripSearchService service;

    @BeforeEach
    void setUp() {

        partnerRepository = new InMemoryTravelPartnerRepository();
        vehicleRepository = new InMemoryVehicleRepository();
        pricingRepository = new InMemoryPricingConfigurationRepository();
        routeService = new RecordingRouteCalculationService();

        TripEligibilityService eligibilityService =
                new TripEligibilityService(
                        new DefaultTravelPartnerEligibilityService(
                                List.of(
                                        new ActiveTravelPartnerRule()
                                )
                        ),
                        vehicleRepository,
                        List.of(
                                new VehicleAvailabilityEligibilityRule(),
                                new PassengerCapacityEligibilityRule(),
                                new VehicleCategoryEligibilityRule(),
                                new VehicleServiceAreaRule()
                        )
                );

        TripPricingService pricingService =
                new TripPricingService(
                        new PricingEngine(
                                List.of(
                                        new OneWayPricingStrategy(),
                                        new RoundTripPricingStrategy(),
                                        new RentalPricingStrategy()
                                )
                        ),
                        pricingRepository,
                        routeService
                );

        service =
                new TripSearchService(
                        partnerRepository,
                        eligibilityService,
                        pricingService
                );
    }

    @Test
    void shouldReturnOnlySelectedTripType() {

        Vehicle vehicle = fullyPricedVehicle(activePartner(), VehicleCategory.MUV, 7);

        List<PricedTravelOption> results =
                service.search(criteria(4, null, TripType.CHAUFFEUR_ROUND_TRIP));

        assertEquals(1, results.size());
        assertEquals(TripType.CHAUFFEUR_ROUND_TRIP, results.getFirst().tripType());
        assertEquals(vehicle.getId(), results.getFirst().vehicle().getId());
    }

    @Test
    void shouldReturnAllApplicableTripTypesWhenNoneSelected() {

        fullyPricedVehicle(activePartner(), VehicleCategory.MUV, 7);

        List<PricedTravelOption> results =
                service.search(criteria(4, null, null));

        assertEquals(
                Set.of(
                        TripType.CHAUFFEUR_ONE_WAY,
                        TripType.CHAUFFEUR_ROUND_TRIP,
                        TripType.CHAUFFEUR_RENTAL
                ),
                results.stream()
                        .map(PricedTravelOption::tripType)
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void shouldReturnOnlySelectedVehicleCategory() {

        TravelPartner partner = activePartner();
        fullyPricedVehicle(partner, VehicleCategory.SEDAN, 4);
        fullyPricedVehicle(partner, VehicleCategory.MUV, 7);

        List<PricedTravelOption> results =
                service.search(criteria(4, VehicleCategory.MUV, TripType.CHAUFFEUR_ONE_WAY));

        assertEquals(1, results.size());
        assertEquals(VehicleCategory.MUV, results.getFirst().vehicle().getCategory());
    }

    @Test
    void shouldReturnAllCategoriesWhenNoneSelected() {

        TravelPartner partner = activePartner();
        fullyPricedVehicle(partner, VehicleCategory.SEDAN, 4);
        fullyPricedVehicle(partner, VehicleCategory.SUV, 6);
        fullyPricedVehicle(partner, VehicleCategory.MUV, 7);

        List<PricedTravelOption> results =
                service.search(criteria(4, null, TripType.CHAUFFEUR_ONE_WAY));

        assertEquals(
                Set.of(VehicleCategory.SEDAN, VehicleCategory.SUV, VehicleCategory.MUV),
                results.stream()
                        .map(option -> option.vehicle().getCategory())
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void shouldExcludeVehicleWithInsufficientCapacity() {

        fullyPricedVehicle(activePartner(), VehicleCategory.SEDAN, 4);

        assertTrue(
                service.search(criteria(6, null, null)).isEmpty()
        );
    }

    @Test
    void shouldExcludeUnavailableVehicle() {

        Vehicle vehicle = fullyPricedVehicle(activePartner(), VehicleCategory.MUV, 7);
        vehicle.makeUnavailable();

        assertTrue(
                service.search(criteria(4, null, null)).isEmpty()
        );
    }

    @Test
    void shouldExcludeInactivePartner() {

        TravelPartner partner = activePartner();
        fullyPricedVehicle(partner, VehicleCategory.MUV, 7);
        partner.suspend();

        assertTrue(
                service.search(criteria(4, null, null)).isEmpty()
        );
    }

    @Test
    void shouldExcludeVehicleWhoseServiceAreaDoesNotCoverPickup() {

        TravelPartner delhiPartner = TravelPartner.create("Delhi Travels");
        delhiPartner.activate();
        partnerRepository.create(delhiPartner);

        Vehicle delhiVehicle =
                fullyPricedVehicle(delhiPartner, VehicleCategory.MUV, 7);

        delhiVehicle.attachServiceAreas(
                List.of(ServiceArea.create("Delhi", 28.6139, 77.2090, 50))
        );

        assertTrue(
                service.search(criteria(4, null, null)).isEmpty()
        );
    }

    @Test
    void shouldExcludeTripTypesWithoutPricingConfiguration() {

        Vehicle vehicle = availableVehicle(activePartner(), VehicleCategory.MUV, 7);
        pricingRepository.save(oneWayConfiguration(vehicle.getId()));

        List<PricedTravelOption> results =
                service.search(criteria(4, null, null));

        assertEquals(1, results.size());
        assertEquals(TripType.CHAUFFEUR_ONE_WAY, results.getFirst().tripType());
    }

    @Test
    void shouldExcludeInactivePricingConfiguration() {

        Vehicle vehicle = availableVehicle(activePartner(), VehicleCategory.MUV, 7);

        PricingConfiguration inactive = oneWayConfiguration(vehicle.getId());
        inactive.deactivate();
        pricingRepository.save(inactive);
        pricingRepository.save(roundTripConfiguration(vehicle.getId()));

        List<PricedTravelOption> results =
                service.search(criteria(4, null, null));

        assertEquals(1, results.size());
        assertEquals(TripType.CHAUFFEUR_ROUND_TRIP, results.getFirst().tripType());
    }

    @Test
    void shouldRouteTheFullMultiStopItinerary() {

        fullyPricedVehicle(activePartner(), VehicleCategory.MUV, 7);

        Location konark = new Location("Konark", 19.8876, 86.0945);
        Location chilika = new Location("Chilika", 19.7167, 85.3167);

        service.search(
                new TripSearchCriteria(
                        new Itinerary(AIRPORT, List.of(konark, chilika), PURI),
                        4,
                        null,
                        TripType.CHAUFFEUR_ONE_WAY
                )
        );

        assertEquals(1, routeService.itineraries.size());
        assertEquals(
                List.of(konark, chilika),
                routeService.itineraries.getFirst().stops()
        );
    }

    @Test
    void shouldPriceUsingBackendRouteDistance() {

        fullyPricedVehicle(activePartner(), VehicleCategory.MUV, 7);

        routeService.distanceKm = BigDecimal.valueOf(100);
        BigDecimal at100Km =
                service.search(criteria(4, null, TripType.CHAUFFEUR_ONE_WAY))
                        .getFirst().price().totalAmount();

        routeService.distanceKm = BigDecimal.valueOf(200);
        BigDecimal at200Km =
                service.search(criteria(4, null, TripType.CHAUFFEUR_ONE_WAY))
                        .getFirst().price().totalAmount();

        assertTrue(at200Km.compareTo(at100Km) > 0);
    }

    private TripSearchCriteria criteria(
            int passengers,
            VehicleCategory category,
            TripType tripType
    ) {

        return new TripSearchCriteria(
                new Itinerary(AIRPORT, List.of(), PURI),
                passengers,
                category,
                tripType
        );
    }

    private TravelPartner activePartner() {

        TravelPartner partner = TravelPartner.create("ABC Travels");
        partner.activate();
        partnerRepository.create(partner);

        return partner;
    }

    private Vehicle availableVehicle(
            TravelPartner partner,
            VehicleCategory category,
            int capacity
    ) {

        Vehicle vehicle =
                Vehicle.create(
                        partner.getId(),
                        "OD02" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                        "Toyota",
                        "Innova",
                        category,
                        capacity
                );
        vehicle.makeAvailable();
        vehicle.attachServiceAreas(
                List.of(ServiceArea.create("Bhubaneswar", 20.2961, 85.8245, 50))
        );
        vehicleRepository.vehicles.add(vehicle);

        return vehicle;
    }

    private Vehicle fullyPricedVehicle(
            TravelPartner partner,
            VehicleCategory category,
            int capacity
    ) {

        Vehicle vehicle = availableVehicle(partner, category, capacity);

        pricingRepository.save(oneWayConfiguration(vehicle.getId()));
        pricingRepository.save(roundTripConfiguration(vehicle.getId()));
        pricingRepository.save(rentalConfiguration(vehicle.getId()));

        return vehicle;
    }

    private OneWayPricingConfiguration oneWayConfiguration(UUID vehicleId) {

        return new OneWayPricingConfiguration(
                UUID.randomUUID(),
                vehicleId,
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(350),
                BigDecimal.valueOf(50)
        );
    }

    private RoundTripPricingConfiguration roundTripConfiguration(UUID vehicleId) {

        return new RoundTripPricingConfiguration(
                UUID.randomUUID(),
                vehicleId,
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(100)
        );
    }

    private RentalPricingConfiguration rentalConfiguration(UUID vehicleId) {

        return new RentalPricingConfiguration(
                UUID.randomUUID(),
                vehicleId,
                Duration.ofHours(8),
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(300)
        );
    }

    private static class RecordingRouteCalculationService
            implements RouteCalculationService {

        final List<Itinerary> itineraries = new ArrayList<>();

        BigDecimal distanceKm = BigDecimal.valueOf(100);

        @Override
        public RouteCalculation calculate(Itinerary itinerary) {

            itineraries.add(itinerary);

            return new RouteCalculation(distanceKm, Duration.ofMinutes(120));
        }
    }

    private static class InMemoryTravelPartnerRepository
            implements TravelPartnerRepository {

        private final List<TravelPartner> partners = new ArrayList<>();

        @Override
        public TravelPartner create(TravelPartner travelPartner) {
            partners.add(travelPartner);
            return travelPartner;
        }

        @Override
        public TravelPartner update(TravelPartner travelPartner) {
            return travelPartner;
        }

        @Override
        public Optional<TravelPartner> findById(UUID id) {
            return partners.stream()
                    .filter(partner -> partner.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<TravelPartner> findActivePartners() {
            return findByStatus(TravelPartnerStatus.ACTIVE);
        }

        @Override
        public List<TravelPartner> findByStatus(TravelPartnerStatus status) {
            return partners.stream()
                    .filter(partner -> partner.getStatus() == status)
                    .toList();
        }

        @Override
        public List<TravelPartner> findAll() {
            return List.copyOf(partners);
        }
    }

    private static class InMemoryVehicleRepository
            implements VehicleRepository {

        final List<Vehicle> vehicles = new ArrayList<>();

        @Override
        public Vehicle create(Vehicle vehicle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vehicle update(Vehicle vehicle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Vehicle> findById(UUID id) {
            return vehicles.stream()
                    .filter(vehicle -> vehicle.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<Vehicle> findByTravelPartnerId(UUID travelPartnerId) {
            return vehicles.stream()
                    .filter(vehicle ->
                            vehicle.getTravelPartnerId().equals(travelPartnerId)
                    )
                    .toList();
        }

        @Override
        public List<Vehicle> findAvailableVehicles() {
            throw new UnsupportedOperationException();
        }
    }

    private static class InMemoryPricingConfigurationRepository
            implements PricingConfigurationRepository {

        private final Map<String, PricingConfiguration> configurations =
                new HashMap<>();

        void save(PricingConfiguration configuration) {
            configurations.put(
                    configuration.getVehicleId() + ":" + configuration.getTripType(),
                    configuration
            );
        }

        @Override
        public PricingConfiguration create(PricingConfiguration configuration) {
            save(configuration);
            return configuration;
        }

        @Override
        public PricingConfiguration update(PricingConfiguration configuration) {
            save(configuration);
            return configuration;
        }

        @Override
        public Optional<PricingConfiguration> findById(UUID id) {
            return configurations.values().stream()
                    .filter(configuration -> configuration.getId().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<PricingConfiguration> findByVehicleIdAndTripType(
                UUID vehicleId,
                TripType tripType
        ) {
            return Optional.ofNullable(
                    configurations.get(vehicleId + ":" + tripType)
            );
        }

        @Override
        public List<PricingConfiguration> findByVehicleId(UUID vehicleId) {
            return configurations.values().stream()
                    .filter(configuration ->
                            configuration.getVehicleId().equals(vehicleId)
                    )
                    .toList();
        }
    }
}
