package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.application.eligibility.VehicleEligibilityRule;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripEligibilityService {

    private final TravelPartnerEligibilityService
            travelPartnerEligibilityService;

    private final VehicleRepository vehicleRepository;

    private final List<VehicleEligibilityRule>
            vehicleEligibilityRules;

    public TripEligibilityService(
            TravelPartnerEligibilityService
                    travelPartnerEligibilityService,
            VehicleRepository vehicleRepository,
            List<VehicleEligibilityRule>
                    vehicleEligibilityRules
    ) {
        this.travelPartnerEligibilityService =
                travelPartnerEligibilityService;

        this.vehicleRepository =
                vehicleRepository;

        this.vehicleEligibilityRules =
                List.copyOf(vehicleEligibilityRules);
    }

    public List<EligibleTravelPartner> findEligibleOptions(
            TripSearchCriteria criteria,
            List<TravelPartner> candidates
    ) {

        if (criteria == null) {
            throw new IllegalArgumentException(
                    "Trip search criteria is required"
            );
        }

        if (candidates == null) {
            throw new IllegalArgumentException(
                    "Travel partner candidates are required"
            );
        }

        List<TravelPartner> eligiblePartners =
                travelPartnerEligibilityService
                        .findEligiblePartners(
                                criteria,
                                candidates
                        );

        return eligiblePartners.stream()
                .map(partner ->
                        createEligiblePartner(
                                partner,
                                criteria
                        )
                )
                .filter(result ->
                        !result.eligibleVehicles().isEmpty()
                )
                .toList();
    }



    public List<EligibleTravelPartner> findEligibleOptions(
            TripRequest tripRequest,
            List<TravelPartner> candidates
    ) {

        if (tripRequest == null) {
            throw new IllegalArgumentException(
                    "Trip request is required"
            );
        }

        TripSearchCriteria criteria =
                new TripSearchCriteria(
                        tripRequest.getItinerary(),
                        tripRequest.getPassengerCount().value(),
                        tripRequest.getVehicleCategory(),
                        tripRequest.getTripType()
                );

        return findEligibleOptions(
                criteria,
                candidates
        );
    }

    private EligibleTravelPartner createEligiblePartner(
            TravelPartner travelPartner,
            TripSearchCriteria criteria
    ) {

        List<Vehicle> vehicles =
                vehicleRepository
                        .findByTravelPartnerId(
                                travelPartner.getId()
                        );

        List<Vehicle> eligibleVehicles =
                vehicles.stream()
                        .filter(vehicle ->
                                vehicleEligibilityRules
                                        .stream()
                                        .allMatch(rule ->
                                                rule.isEligible(
                                                        vehicle,
                                                        criteria
                                                )
                                        )
                        )
                        .toList();

        return new EligibleTravelPartner(
                travelPartner,
                eligibleVehicles
        );
    }
}