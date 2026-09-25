package com.yocabs.api.modules.tripsearch.application;

import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.application.service.TripPricingService;
import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.application.service.TripEligibilityService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Re-validates, at action time, that a search result the tourist picked
 * (vehicle + trip type) is still eligible and priced. Search results are
 * not a guarantee of availability.
 */
@Service
public class SelectedOptionService {

    private static final String NO_LONGER_AVAILABLE =
            "The selected option is no longer available for this trip";

    private final TravelPartnerRepository travelPartnerRepository;
    private final VehicleRepository vehicleRepository;
    private final TripEligibilityService tripEligibilityService;
    private final TripPricingService tripPricingService;

    public SelectedOptionService(
            TravelPartnerRepository travelPartnerRepository,
            VehicleRepository vehicleRepository,
            TripEligibilityService tripEligibilityService,
            TripPricingService tripPricingService
    ) {
        this.travelPartnerRepository = travelPartnerRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripEligibilityService = tripEligibilityService;
        this.tripPricingService = tripPricingService;
    }

    public PricedTravelOption resolve(
            TripRequest tripRequest,
            UUID vehicleId,
            TripType tripType
    ) {

        if (tripRequest == null) {
            throw new IllegalArgumentException("Trip request is required");
        }

        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle is required");
        }

        if (tripType == null) {
            throw new IllegalArgumentException("Trip type is required");
        }

        if (tripRequest.getTripType() != null && tripRequest.getTripType() != tripType) {
            throw new IllegalArgumentException(
                    "Trip type does not match the trip request"
            );
        }

        Vehicle vehicle =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        TravelPartner partner =
                travelPartnerRepository.findById(vehicle.getTravelPartnerId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Travel partner not found: " + vehicle.getTravelPartnerId()));

        TripSearchCriteria base = TripSearchCriteria.from(tripRequest);

        TripSearchCriteria criteria =
                new TripSearchCriteria(
                        base.itinerary(),
                        base.passengerCount(),
                        base.vehicleCategory(),
                        tripType
                );

        Vehicle eligibleVehicle =
                tripEligibilityService.findEligibleOptions(criteria, List.of(partner))
                        .stream()
                        .flatMap(eligible -> eligible.eligibleVehicles().stream())
                        .filter(candidate -> candidate.getId().equals(vehicleId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(NO_LONGER_AVAILABLE));

        return tripPricingService
                .calculatePrices(
                        criteria,
                        List.of(new EligibleTravelPartner(partner, List.of(eligibleVehicle)))
                )
                .stream()
                .filter(option -> option.tripType() == tripType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(NO_LONGER_AVAILABLE));
    }
}
