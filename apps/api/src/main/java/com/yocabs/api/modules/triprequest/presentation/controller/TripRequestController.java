package com.yocabs.api.modules.triprequest.presentation.controller;

import com.yocabs.api.modules.triprequest.application.command.CreateTripRequestCommand;
import com.yocabs.api.modules.triprequest.application.service.CreateTripRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trip-requests")
public class TripRequestController {

    private final CreateTripRequestService createTripRequestService;

    public TripRequestController(
            CreateTripRequestService createTripRequestService
    ) {
        this.createTripRequestService = createTripRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(@RequestBody CreateTripRequestRequest request) {

        var command = new CreateTripRequestCommand(
                request.touristId(),
                request.pickupDescription(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.stops(),
                request.destinationDescription(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.startDate(),
                request.endDate(),
                request.passengerCount(),
                request.tripBrief()
        );

        var tripRequest = createTripRequestService.execute(command);

        return tripRequest.getId().value();
    }

    public record CreateTripRequestRequest(
            UUID touristId,
            String pickupDescription,
            Double pickupLatitude,
            Double pickupLongitude,
            List<CreateTripRequestCommand.LocationCommand> stops,
            String destinationDescription,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDate startDate,
            LocalDate endDate,
            int passengerCount,
            String tripBrief
    ) {
    }
}