package com.yocabs.api.modules.triprequest.presentation.controller;

import com.yocabs.api.modules.triprequest.application.command.CreateTripRequestCommand;
import com.yocabs.api.modules.triprequest.application.service.CreateTripRequestService;
import com.yocabs.api.modules.triprequest.application.service.GetTripRequestService;
import com.yocabs.api.modules.triprequest.application.service.SubmitTripRequestService;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.presentation.dto.TripRequestResponse;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import com.yocabs.api.shared.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
    public UUID create(
            @CurrentActor Actor actor,
            @RequestBody CreateTripRequestRequest request
    ) {

        actor.requireRole(Role.TOURIST);

        var command = new CreateTripRequestCommand(
                actor.userId(),
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
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {
        TripRequest tripRequest = getTripRequestService.execute(id);
        requireAccess(actor, tripRequest);

        return TripRequestResponse.from(tripRequest);
    }

    @PostMapping("/{id}/submit")
    public TripRequestResponse submit(
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {
        requireAccess(actor, getTripRequestService.execute(id));

        return TripRequestResponse.from(
                submitTripRequestService.execute(id)
        );
    }

    private static void requireAccess(Actor actor, TripRequest tripRequest) {
        if (actor.isAdmin()) {
            return;
        }
        if (actor.role() == Role.TOURIST
                && actor.userId().equals(tripRequest.getTouristId().value())) {
            return;
        }
        throw new AccessDeniedException("Not permitted for this trip request");
    }


    public record CreateTripRequestRequest(
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
