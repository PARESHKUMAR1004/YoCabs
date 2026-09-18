package com.yocabs.api.modules.triprequest.presentation.dto;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripRequestResponse(
        UUID id,
        UUID touristId,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        int passengerCount,
        String tripBrief,
        TripType tripType,
        VehicleCategory vehicleCategory,
        LocationResponse pickup,
        List<LocationResponse> stops,
        LocationResponse destination,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    public static TripRequestResponse from(
            TripRequest tripRequest
    ) {
        return new TripRequestResponse(
                tripRequest.getId().value(),
                tripRequest.getTouristId().value(),
                tripRequest.getStatus().name(),
                tripRequest.getTravelDateRange().startDate(),
                tripRequest.getTravelDateRange().endDate(),
                tripRequest.getPassengerCount().value(),
                tripRequest.getTripBrief().value(),
                tripRequest.getTripType(),
                tripRequest.getVehicleCategory(),
                LocationResponse.from(
                        tripRequest.getItinerary().pickup()
                ),
                tripRequest.getItinerary()
                        .stops()
                        .stream()
                        .map(LocationResponse::from)
                        .toList(),
                LocationResponse.from(
                        tripRequest.getItinerary().destination()
                ),
                tripRequest.getVersion(),
                tripRequest.getCreatedAt(),
                tripRequest.getUpdatedAt()
        );
    }

    public record LocationResponse(
            String description,
            Double latitude,
            Double longitude
    ) {

        public static LocationResponse from(
                com.yocabs.api.modules.triprequest.domain.valueobject.Location location
        ) {
            return new LocationResponse(
                    location.description(),
                    location.latitude(),
                    location.longitude()
            );
        }
    }
}