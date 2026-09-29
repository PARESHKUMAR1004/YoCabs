package com.yocabs.api.modules.tripsearch.interfaces.rest;

import com.yocabs.api.modules.pricing.interfaces.rest.TripPricingController.PriceCalculationResponse;
import com.yocabs.api.modules.review.application.ReviewService;
import com.yocabs.api.modules.standardrate.application.StandardRateService;
import com.yocabs.api.modules.standardrate.domain.StandardRate;
import com.yocabs.api.modules.review.domain.ReviewRepository.RatingSummary;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.tripsearch.application.TripSearchService;
import com.yocabs.api.modules.tripsearch.application.TripSearchService.TripSearchResult;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.vehicle.application.service.VehicleShowcaseService;
import com.yocabs.api.modules.vehicle.application.service.VehicleShowcaseService.Showcase;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trip-search")
public class TripSearchController {

    private final TripSearchService tripSearchService;
    private final ReviewService reviewService;
    private final VehicleShowcaseService showcaseService;
    private final StandardRateService standardRateService;

    public TripSearchController(
            TripSearchService tripSearchService,
            ReviewService reviewService,
            VehicleShowcaseService showcaseService,
            StandardRateService standardRateService
    ) {
        this.tripSearchService = tripSearchService;
        this.reviewService = reviewService;
        this.showcaseService = showcaseService;
        this.standardRateService = standardRateService;
    }

    @PostMapping
    public TripSearchResponse search(
            @RequestBody TripSearchRequest request
    ) {

        TripSearchResult result =
                tripSearchService.searchWithRoute(request.toCriteria());

        Map<UUID, RatingSummary> ratings = new HashMap<>();
        Map<com.yocabs.api.modules.vehicle.domain.model.VehicleCategory, StandardRate> standardRates =
                standardRateService.byCategory();

        Map<UUID, Showcase> showcases =
                showcaseService.forVehicles(
                        result.options().stream().map(option -> option.vehicle().getId()).distinct().toList()
                );

        List<SearchOptionResponse> options =
                result.options().stream()
                        .map(option -> {
                            RatingSummary rating =
                                    ratings.computeIfAbsent(
                                            option.travelPartner().getId(),
                                            reviewService::summary
                                    );

                            StandardRate standardRate =
                                    standardRates.get(option.vehicle().getCategory());

                            BigDecimal standardAmount =
                                    standardRate == null
                                            ? null
                                            : standardRate.perKmRate()
                                                    .multiply(result.route().distanceKm())
                                                    .setScale(2, java.math.RoundingMode.HALF_UP);

                            return SearchOptionResponse.from(
                                    option, rating, showcases.get(option.vehicle().getId()), standardAmount);
                        })
                        .toList();

        return new TripSearchResponse(
                result.route().distanceKm(),
                result.route().duration().toMinutes(),
                options
        );
    }

    public record TripSearchResponse(
            BigDecimal distanceKm,
            long durationMinutes,
            List<SearchOptionResponse> options
    ) {
    }

    public record SearchOptionResponse(
            UUID travelPartnerId,
            String travelPartnerName,
            double rating,
            long reviewCount,
            UUID vehicleId,
            String registrationNumber,
            String make,
            String model,
            String category,
            int passengerCapacity,
            String fuelType,
            String transmission,
            Integer modelYear,
            Integer luggageCapacity,
            List<FacilityResponse> facilities,
            List<String> photos,
            String tripType,
            PriceCalculationResponse price,
            /** What YoCabs considers a typical fare for this vehicle type over this distance; null when no admin rate is set. */
            BigDecimal standardAmount
    ) {

        static SearchOptionResponse from(
                com.yocabs.api.modules.pricing.application.model.PricedTravelOption option,
                RatingSummary rating,
                Showcase showcase,
                BigDecimal standardAmount
        ) {
            var vehicle = option.vehicle();

            return new SearchOptionResponse(
                    option.travelPartner().getId(),
                    option.travelPartner().getName(),
                    rating.average(),
                    rating.count(),
                    vehicle.getId(),
                    vehicle.getRegistrationNumber(),
                    vehicle.getMake(),
                    vehicle.getModel(),
                    vehicle.getCategory().name(),
                    vehicle.getPassengerCapacity(),
                    showcase.fuelType() == null ? null : showcase.fuelType().name(),
                    showcase.transmission() == null ? null : showcase.transmission().name(),
                    showcase.modelYear(),
                    showcase.luggageCapacity(),
                    showcase.facilities().stream()
                            .map(facility -> new FacilityResponse(facility.code(), facility.name()))
                            .toList(),
                    showcase.photoIds().stream()
                            .map(id -> "/api/v1/vehicles/" + vehicle.getId() + "/photos/" + id)
                            .toList(),
                    option.tripType().name(),
                    PriceCalculationResponse.from(option.price()),
                    standardAmount
            );
        }
    }

    public record FacilityResponse(String code, String name) {
    }

    public record TripSearchRequest(
            LocationRequest pickup,
            LocationRequest destination,
            List<LocationRequest> stops,
            LocalDate startDate,
            LocalDate endDate,
            Integer passengerCount,
            VehicleCategory vehicleCategory,
            TripType tripType
    ) {

        TripSearchCriteria toCriteria() {

            // Validates the travel dates; availability by date is not yet modelled.
            new TravelDateRange(
                    startDate,
                    endDate
            );

            if (passengerCount == null) {
                throw new IllegalArgumentException(
                        "Passenger count is required"
                );
            }

            List<Location> stopLocations =
                    stops == null
                            ? List.of()
                            : stops.stream()
                            .map(LocationRequest::toDomain)
                            .toList();

            return new TripSearchCriteria(
                    new Itinerary(
                            LocationRequest.toDomain(pickup),
                            stopLocations,
                            LocationRequest.toDomain(destination)
                    ),
                    passengerCount,
                    vehicleCategory,
                    tripType
            );
        }
    }

    public record LocationRequest(
            String description,
            Double latitude,
            Double longitude
    ) {

        static Location toDomain(
                LocationRequest request
        ) {

            if (request == null) {
                return null;
            }

            return new Location(
                    request.description(),
                    request.latitude(),
                    request.longitude()
            );
        }
    }
}
