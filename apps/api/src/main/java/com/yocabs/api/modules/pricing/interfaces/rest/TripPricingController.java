package com.yocabs.api.modules.pricing.interfaces.rest;

import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.application.service.TripPricingService;
import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.application.service.TripEligibilityService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trip-requests")
public class TripPricingController {

    private final TripRequestRepository tripRequestRepository;

    private final TravelPartnerRepository
            travelPartnerRepository;

    private final TripEligibilityService
            tripEligibilityService;

    private final TripPricingService
            tripPricingService;

    public TripPricingController(
            TripRequestRepository tripRequestRepository,
            TravelPartnerRepository travelPartnerRepository,
            TripEligibilityService tripEligibilityService,
            TripPricingService tripPricingService
    ) {
        this.tripRequestRepository =
                tripRequestRepository;

        this.travelPartnerRepository =
                travelPartnerRepository;

        this.tripEligibilityService =
                tripEligibilityService;

        this.tripPricingService =
                tripPricingService;
    }

    @PostMapping("/{tripRequestId}/priced-options")
    public List<PricedTravelOptionResponse>
    getPricedOptions(
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

        List<EligibleTravelPartner> eligiblePartners =
                tripEligibilityService
                        .findEligibleOptions(
                                tripRequest,
                                candidates
                        );



        return tripPricingService
                .calculatePrices(
                        tripRequest,
                        eligiblePartners
                )
                .stream()
                .map(
                        PricedTravelOptionResponse::from
                )
                .toList();
    }



    public record PricedTravelOptionResponse(
            UUID travelPartnerId,
            String travelPartnerName,
            UUID vehicleId,
            String registrationNumber,
            String make,
            String model,
            String category,
            int passengerCapacity,
            String tripType,
            PriceCalculationResponse price
    ) {

        public static PricedTravelOptionResponse from(
                PricedTravelOption option
        ) {

            Vehicle vehicle =
                    option.vehicle();

            return new PricedTravelOptionResponse(
                    option.travelPartner().getId(),
                    option.travelPartner().getName(),
                    vehicle.getId(),
                    vehicle.getRegistrationNumber(),
                    vehicle.getMake(),
                    vehicle.getModel(),
                    vehicle.getCategory().name(),
                    vehicle.getPassengerCapacity(),
                    option.tripType().name(),
                    PriceCalculationResponse.from(
                            option.price()
                    )
            );
        }
    }

    public record PriceCalculationResponse(
            BigDecimal totalAmount,
            String currency,
            List<PriceComponentResponse> components
    ) {

        public static PriceCalculationResponse from(
                PriceCalculation calculation
        ) {

            return new PriceCalculationResponse(
                    calculation.totalAmount(),
                    calculation.currency(),
                    calculation.components()
                            .stream()
                            .map(component ->
                                    new PriceComponentResponse(
                                            component.code(),
                                            component.description(),
                                            component.amount()
                                    )
                            )
                            .toList()
            );
        }
    }

    public record PriceComponentResponse(
            String code,
            String description,
            BigDecimal amount
    ) {
    }
}