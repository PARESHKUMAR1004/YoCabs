package com.yocabs.api.modules.triprequest.application.command;

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
        String tripBrief
) {

    public record LocationCommand(
            String description,
            Double latitude,
            Double longitude
    ) {
    }
}