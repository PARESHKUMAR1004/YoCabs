package com.yocabs.api.modules.pricing.application.service;

import com.yocabs.api.modules.pricing.application.PricingEngine;
import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripPricingService {

    private final PricingEngine pricingEngine;

    private final PricingConfigurationRepository
            pricingConfigurationRepository;

    private final RouteCalculationService
            routeCalculationService;

    public TripPricingService(
            PricingEngine pricingEngine,
            PricingConfigurationRepository
                    pricingConfigurationRepository,
            RouteCalculationService
                    routeCalculationService
    ) {

        this.pricingEngine = pricingEngine;

        this.pricingConfigurationRepository =
                pricingConfigurationRepository;

        this.routeCalculationService =
                routeCalculationService;
    }

    public List<PricedTravelOption> calculatePrices(
            TripRequest tripRequest,
            List<EligibleTravelPartner> eligiblePartners
    ) {

        if (tripRequest == null) {
            throw new IllegalArgumentException(
                    "Trip request is required"
            );
        }

        return calculatePrices(
                TripSearchCriteria.from(tripRequest),
                eligiblePartners
        );
    }

    public List<PricedTravelOption> calculatePrices(
            TripSearchCriteria criteria,
            List<EligibleTravelPartner> eligiblePartners
    ) {

        return price(criteria, eligiblePartners).options();
    }

    /** Prices the options and also returns the route they were priced on. */
    public PricingOutcome price(
            TripSearchCriteria criteria,
            List<EligibleTravelPartner> eligiblePartners
    ) {

        validateInput(
                criteria,
                eligiblePartners
        );

        RouteCalculation routeCalculation =
                routeCalculationService.calculate(
                        criteria.itinerary()
                );

        PricingContext pricingContext =
                new PricingContext(
                        routeCalculation.distanceKm(),
                        routeCalculation.duration(),
                        null,
                        null
                );

        List<TripType> tripTypes =
                determineTripTypes(criteria);

        List<PricedTravelOption> results =
                new ArrayList<>();

        for (EligibleTravelPartner partner :
                eligiblePartners) {

            for (Vehicle vehicle :
                    partner.eligibleVehicles()) {

                for (TripType tripType : tripTypes) {

                    PricingConfiguration configuration =
                            pricingConfigurationRepository
                                    .findByVehicleIdAndTripType(
                                            vehicle.getId(),
                                            tripType
                                    )
                                    .orElse(null);

                    /*
                     * A vehicle is eligible for the trip,
                     * but it may not have pricing configured
                     * for every trip type.
                     *
                     * In that case, simply don't produce
                     * a priced option for that combination.
                     */
                    if (configuration == null) {
                        continue;
                    }

                    if (!configuration.isActive()) {
                        continue;
                    }

                    var calculation =
                            pricingEngine.calculate(
                                    tripType,
                                    pricingContext,
                                    vehicle,
                                    configuration
                            );

                    results.add(
                            new PricedTravelOption(
                                    partner.travelPartner(),
                                    vehicle,
                                    tripType,
                                    calculation
                            )
                    );
                }
            }
        }

        return new PricingOutcome(
                routeCalculation,
                List.copyOf(results)
        );
    }

    public record PricingOutcome(
            RouteCalculation route,
            List<PricedTravelOption> options
    ) {
    }

    private List<TripType> determineTripTypes(
            TripSearchCriteria criteria
    ) {

        if (criteria.tripType() != null) {

            return List.of(
                    criteria.tripType()
            );
        }

        return List.of(
                TripType.CHAUFFEUR_ONE_WAY,
                TripType.CHAUFFEUR_ROUND_TRIP,
                TripType.CHAUFFEUR_RENTAL
        );
    }

    private void validateInput(
            TripSearchCriteria criteria,
            List<EligibleTravelPartner> eligiblePartners
    ) {

        if (criteria == null) {
            throw new IllegalArgumentException(
                    "Trip search criteria is required"
            );
        }

        if (eligiblePartners == null) {
            throw new IllegalArgumentException(
                    "Eligible travel partners are required"
            );
        }
    }
}