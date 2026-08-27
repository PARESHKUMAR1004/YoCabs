package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
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
            TripRequest tripRequest,
            List<TravelPartner> candidates
    ) {

        if (tripRequest == null) {
            throw new IllegalArgumentException(
                    "Trip request is required"
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
                                tripRequest,
                                candidates
                        );

        return eligiblePartners.stream()
                .map(partner ->
                        createEligiblePartner(
                                partner,
                                tripRequest
                        )
                )
                .filter(result ->
                        !result.eligibleVehicles().isEmpty()
                )
                .toList();
    }

    private EligibleTravelPartner createEligiblePartner(
            TravelPartner travelPartner,
            TripRequest tripRequest
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
                                                        tripRequest
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