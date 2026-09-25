package com.yocabs.api.modules.travelpartner.presentation.controller;

import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.application.service.TripEligibilityService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trip-requests")
public class TripEligibilityController {

    private final TripRequestRepository tripRequestRepository;

    private final TravelPartnerRepository
            travelPartnerRepository;

    private final TripEligibilityService
            tripEligibilityService;

    public TripEligibilityController(
            TripRequestRepository tripRequestRepository,
            TravelPartnerRepository travelPartnerRepository,
            TripEligibilityService tripEligibilityService
    ) {
        this.tripRequestRepository =
                tripRequestRepository;

        this.travelPartnerRepository =
                travelPartnerRepository;

        this.tripEligibilityService =
                tripEligibilityService;
    }

    @GetMapping("/{tripRequestId}/eligible-options")
    public List<EligibleOptionResponse> getEligibleOptions(
            @CurrentActor Actor actor,
            @PathVariable UUID tripRequestId
    ) {

        TripRequest tripRequest =
                tripRequestRepository
                        .findById(
                                new TripRequestId(
                                        tripRequestId
                                )
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Trip request not found: "
                                                + tripRequestId
                                )
                        );

        actor.requireTouristOwnerOrAdmin(
                tripRequest.getTouristId().value()
        );

        List<TravelPartner> candidates =
                travelPartnerRepository
                        .findActivePartners();

        return tripEligibilityService
                .findEligibleOptions(
                        tripRequest,
                        candidates
                )
                .stream()
                .map(
                        EligibleOptionResponse::from
                )
                .toList();
    }

    public record EligibleOptionResponse(
            UUID travelPartnerId,
            String travelPartnerName,
            List<EligibleVehicleResponse> vehicles
    ) {

        public static EligibleOptionResponse from(
                EligibleTravelPartner eligiblePartner
        ) {

            return new EligibleOptionResponse(
                    eligiblePartner
                            .travelPartner()
                            .getId(),

                    eligiblePartner
                            .travelPartner()
                            .getName(),

                    eligiblePartner
                            .eligibleVehicles()
                            .stream()
                            .map(
                                    vehicle ->
                                            new EligibleVehicleResponse(
                                                    vehicle.getId(),
                                                    vehicle.getRegistrationNumber(),
                                                    vehicle.getMake(),
                                                    vehicle.getModel(),
                                                    vehicle.getCategory(),
                                                    vehicle.getPassengerCapacity()
                                            )
                            )
                            .toList()
            );
        }
    }

    public record EligibleVehicleResponse(
            UUID vehicleId,
            String registrationNumber,
            String make,
            String model,
            String category,
            int passengerCapacity
    ) {

        public EligibleVehicleResponse(
                UUID vehicleId,
                String registrationNumber,
                String make,
                String model,
                com.yocabs.api.modules.vehicle.domain.model.VehicleCategory category,
                int passengerCapacity
        ) {
            this(
                    vehicleId,
                    registrationNumber,
                    make,
                    model,
                    category.name(),
                    passengerCapacity
            );
        }
    }
}