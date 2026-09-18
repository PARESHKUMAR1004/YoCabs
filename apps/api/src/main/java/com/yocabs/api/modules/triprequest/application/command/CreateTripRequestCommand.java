package com.yocabs.api.modules.triprequest.application.command;

import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTripRequestCommand(
        UUID touristId,
        String pickupDescription,
        Double pickupLatitude,
        Double pickupLongitude,
        List<LocationCommand> stops,
        String destinationDescription,
        Double destinationLatitude,
        Double destinationLongitude,
        LocalDate startDate,
        LocalDate endDate,
        int passengerCount,
        String tripBrief,
        TripType tripType,
        VehicleCategory vehicleCategory
) {

    public record LocationCommand(
            String description,
            Double latitude,
            Double longitude
    ) {
    }
}