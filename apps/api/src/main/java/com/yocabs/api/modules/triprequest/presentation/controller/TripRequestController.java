package com.yocabs.api.modules.triprequest.presentation.controller;

import com.yocabs.api.modules.triprequest.application.command.CreateTripRequestCommand;
import com.yocabs.api.modules.triprequest.application.service.CreateTripRequestService;
import com.yocabs.api.modules.triprequest.application.service.GetTripRequestService;
import com.yocabs.api.modules.triprequest.application.service.SubmitTripRequestService;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.presentation.dto.TripRequestResponse;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trip-requests")
public class TripRequestController {

    private final CreateTripRequestService createTripRequestService;
    private final GetTripRequestService getTripRequestService;
    private final SubmitTripRequestService submitTripRequestService;

    public TripRequestController(
            CreateTripRequestService createTripRequestService,
            GetTripRequestService getTripRequestService,
            SubmitTripRequestService submitTripRequestService
    ) {
        this.createTripRequestService = createTripRequestService;
        this.getTripRequestService = getTripRequestService;
        this.submitTripRequestService = submitTripRequestService;
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
                request.tripBrief(),
                request.tripType(),
                request.vehicleCategory()

        );

        var tripRequest = createTripRequestService.execute(command);

        return tripRequest.getId().value();
    }

    @GetMapping("/{id}")
    public TripRequestResponse getById(
            @PathVariable UUID id
    ) {
        return TripRequestResponse.from(
                getTripRequestService.execute(id)
        );
    }

    @PostMapping("/{id}/submit")
    public TripRequestResponse submit(
            @PathVariable UUID id
    ) {
        return TripRequestResponse.from(
                submitTripRequestService.execute(id)
        );
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
            String tripBrief,
            TripType tripType,
            VehicleCategory vehicleCategory
    ) {
    }
}