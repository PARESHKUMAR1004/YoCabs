package com.yocabs.api.modules.pricing.application.service;

import com.yocabs.api.modules.pricing.application.PricingEngine;
import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.application.strategy.OneWayPricingStrategy;
import com.yocabs.api.modules.pricing.application.strategy.RentalPricingStrategy;
import com.yocabs.api.modules.pricing.application.strategy.RoundTripPricingStrategy;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.pricing.domain.model.OneWayPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RentalPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RoundTripPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.routing.infrastructure.TestRouteCalculationService;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.PassengerCount;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripBrief;
import com.yocabs.api.modules.triprequest.domain.valueobject.TouristId;
import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripPricingServiceTest {

    private final InMemoryPricingConfigurationRepository
            pricingConfigurationRepository =
            new InMemoryPricingConfigurationRepository();

    private final PricingEngine pricingEngine =
            new PricingEngine(
                    List.of(
                            new OneWayPricingStrategy(),
                            new RoundTripPricingStrategy(),
                            new RentalPricingStrategy()
                    )
            );

    private final TestRouteCalculationService
            routeCalculationService =
            new TestRouteCalculationService();

    private final TripPricingService service =
            new TripPricingService(
                    pricingEngine,
                    pricingConfigurationRepository,
                    routeCalculationService
            );

    @Test
    void shouldCalculateOnlySelectedTripType() {

        TravelPartner partner =
                TravelPartner.create("ABC Travels");

        partner.activate();

        Vehicle vehicle =
                Vehicle.create(
                        partner.getId(),
                        "OD02AB1234",
                        "Toyota",
                        "Innova Crysta",
                        VehicleCategory.MUV,
                        7
                );

        vehicle.makeAvailable();

        pricingConfigurationRepository.save(
                oneWayConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                roundTripConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                rentalConfiguration(
                        vehicle.getId()
                )
        );

        TripRequest tripRequest =
                createTripRequest(
                        TripType.CHAUFFEUR_ONE_WAY
                );

        EligibleTravelPartner eligiblePartner =
                new EligibleTravelPartner(
                        partner,
                        List.of(vehicle)
                );

        PricingContext pricingContext =
                new PricingContext(
                        BigDecimal.valueOf(100),
                        Duration.ofHours(8),
                        null,
                        null
                );

        List<PricedTravelOption> results =
                service.calculatePrices(
                        tripRequest,
                        List.of(eligiblePartner)
                );

        assertEquals(
                1,
                results.size()
        );

        assertEquals(
                TripType.CHAUFFEUR_ONE_WAY,
                results.getFirst().tripType()
        );

        assertEquals(
                vehicle.getId(),
                results.getFirst()
                        .vehicle()
                        .getId()
        );

        assertEquals(
                partner.getId(),
                results.getFirst()
                        .travelPartner()
                        .getId()
        );
    }

    @Test
    void shouldCalculateAllActiveTripTypesWhenNoPreferenceIsSelected() {

        TravelPartner partner =
                TravelPartner.create("ABC Travels");

        partner.activate();

        Vehicle vehicle =
                Vehicle.create(
                        partner.getId(),
                        "OD02AB1234",
                        "Toyota",
                        "Innova Crysta",
                        VehicleCategory.MUV,
                        7
                );

        vehicle.makeAvailable();

        pricingConfigurationRepository.save(
                oneWayConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                roundTripConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                rentalConfiguration(
                        vehicle.getId()
                )
        );

        TripRequest tripRequest =
                createTripRequest(null);

        EligibleTravelPartner eligiblePartner =
                new EligibleTravelPartner(
                        partner,
                        List.of(vehicle)
                );

        PricingContext pricingContext =
                new PricingContext(
                        BigDecimal.valueOf(100),
                        Duration.ofHours(8),
                        null,
                        null
                );

        List<PricedTravelOption> results =
                service.calculatePrices(
                        tripRequest,
                        List.of(eligiblePartner)
                );

        assertEquals(
                3,
                results.size()
        );

        assertTrue(
                results.stream()
                        .anyMatch(option ->
                                option.tripType()
                                        == TripType.CHAUFFEUR_ONE_WAY
                        )
        );

        assertTrue(
                results.stream()
                        .anyMatch(option ->
                                option.tripType()
                                        == TripType.CHAUFFEUR_ROUND_TRIP
                        )
        );

        assertTrue(
                results.stream()
                        .anyMatch(option ->
                                option.tripType()
                                        == TripType.CHAUFFEUR_RENTAL
                        )
        );
    }

    @Test
    void shouldSkipMissingPricingConfiguration() {

        TravelPartner partner =
                TravelPartner.create("ABC Travels");

        partner.activate();

        Vehicle vehicle =
                Vehicle.create(
                        partner.getId(),
                        "OD02AB1234",
                        "Toyota",
                        "Innova Crysta",
                        VehicleCategory.MUV,
                        7
                );

        vehicle.makeAvailable();

        pricingConfigurationRepository.save(
                oneWayConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                roundTripConfiguration(
                        vehicle.getId()
                )
        );

        /*
         * No rental pricing configuration.
         */

        TripRequest tripRequest =
                createTripRequest(null);

        EligibleTravelPartner eligiblePartner =
                new EligibleTravelPartner(
                        partner,
                        List.of(vehicle)
                );

        PricingContext pricingContext =
                new PricingContext(
                        BigDecimal.valueOf(100),
                        Duration.ofHours(8),
                        null,
                        null
                );

        List<PricedTravelOption> results =
                service.calculatePrices(
                        tripRequest,
                        List.of(eligiblePartner)
                );

        assertEquals(
                2,
                results.size()
        );

        assertTrue(
                results.stream()
                        .noneMatch(option ->
                                option.tripType()
                                        == TripType.CHAUFFEUR_RENTAL
                        )
        );
    }

    @Test
    void shouldSkipInactivePricingConfiguration() {

        TravelPartner partner =
                TravelPartner.create("ABC Travels");

        partner.activate();

        Vehicle vehicle =
                Vehicle.create(
                        partner.getId(),
                        "OD02AB1234",
                        "Toyota",
                        "Innova Crysta",
                        VehicleCategory.MUV,
                        7
                );

        vehicle.makeAvailable();

        PricingConfiguration rental =
                rentalConfiguration(
                        vehicle.getId()
                );

        rental.deactivate();

        pricingConfigurationRepository.save(
                oneWayConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                roundTripConfiguration(
                        vehicle.getId()
                )
        );

        pricingConfigurationRepository.save(
                rental
        );

        TripRequest tripRequest =
                createTripRequest(null);

        EligibleTravelPartner eligiblePartner =
                new EligibleTravelPartner(
                        partner,
                        List.of(vehicle)
                );

        PricingContext pricingContext =
                new PricingContext(
                        BigDecimal.valueOf(100),
                        Duration.ofHours(8),
                        null,
                        null
                );

        List<PricedTravelOption> results =
                service.calculatePrices(
                        tripRequest,
                        List.of(eligiblePartner)
                );

        assertEquals(
                2,
                results.size()
        );

        assertTrue(
                results.stream()
                        .noneMatch(option ->
                                option.tripType()
                                        == TripType.CHAUFFEUR_RENTAL
                        )
        );
    }

    private TripRequest createTripRequest(
            TripType tripType
    ) {

        UUID touristId =
                UUID.randomUUID();

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
                new TouristId(touristId),
                itinerary,
                new TravelDateRange(
                        LocalDate.of(
                                2026,
                                8,
                                27
                        ),
                        LocalDate.of(
                                2026,
                                8,
                                27
                        )
                ),
                new PassengerCount(4),
                new TripBrief(
                        "Airport to Puri"
                ),
                tripType,
                VehicleCategory.SEDAN
        );
    }

    private OneWayPricingConfiguration
    oneWayConfiguration(
            UUID vehicleId
    ) {

        return new OneWayPricingConfiguration(
                UUID.randomUUID(),
                vehicleId,
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(350),
                BigDecimal.valueOf(50)
        );
    }

    private RoundTripPricingConfiguration
    roundTripConfiguration(
            UUID vehicleId
    ) {

        return new RoundTripPricingConfiguration(
                UUID.randomUUID(),
                vehicleId,
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(100)
        );
    }

    private RentalPricingConfiguration
    rentalConfiguration(
            UUID vehicleId
    ) {

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

    private static class InMemoryPricingConfigurationRepository
            implements PricingConfigurationRepository {

        private final Map<String, PricingConfiguration>
                configurations =
                new HashMap<>();

        void save(
                PricingConfiguration configuration
        ) {

            configurations.put(
                    key(
                            configuration.getVehicleId(),
                            configuration.getTripType()
                    ),
                    configuration
            );
        }

        @Override
        public PricingConfiguration create(
                PricingConfiguration configuration
        ) {

            save(configuration);
            return configuration;
        }

        @Override
        public PricingConfiguration update(
                PricingConfiguration configuration
        ) {

            save(configuration);
            return configuration;
        }

        @Override
        public Optional<PricingConfiguration> findById(
                UUID id
        ) {

            return configurations.values()
                    .stream()
                    .filter(configuration ->
                            configuration.getId()
                                    .equals(id)
                    )
                    .findFirst();
        }

        @Override
        public Optional<PricingConfiguration>
        findByVehicleIdAndTripType(
                UUID vehicleId,
                TripType tripType
        ) {

            return Optional.ofNullable(
                    configurations.get(
                            key(
                                    vehicleId,
                                    tripType
                            )
                    )
            );
        }

        @Override
        public List<PricingConfiguration>
        findByVehicleId(
                UUID vehicleId
        ) {

            return configurations.values()
                    .stream()
                    .filter(configuration ->
                            configuration.getVehicleId()
                                    .equals(vehicleId)
                    )
                    .toList();
        }

        private static String key(
                UUID vehicleId,
                TripType tripType
        ) {

            return vehicleId + ":" + tripType;
        }
    }
}