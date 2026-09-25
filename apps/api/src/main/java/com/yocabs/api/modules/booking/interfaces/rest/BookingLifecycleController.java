package com.yocabs.api.modules.booking.interfaces.rest;

import com.yocabs.api.modules.booking.application.BookingLifecycleService;
import com.yocabs.api.modules.booking.interfaces.rest.BookingController.BookingResponse;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}")
public class BookingLifecycleController {

    private final BookingLifecycleService lifecycleService;
    private final BookingViewAssembler assembler;

    public BookingLifecycleController(
            BookingLifecycleService lifecycleService,
            BookingViewAssembler assembler
    ) {
        this.lifecycleService = lifecycleService;
        this.assembler = assembler;
    }

    @PostMapping("/driver")
    public BookingResponse assignDriver(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId,
            @RequestBody AssignDriverRequest request
    ) {
        return assembler.toResponse(
                lifecycleService.assignDriver(actor, bookingId, request.driverId()), actor
        );
    }

    @PostMapping("/start")
    public BookingResponse start(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return assembler.toResponse(lifecycleService.startTrip(actor, bookingId), actor);
    }

    @PostMapping("/complete")
    public BookingResponse complete(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return assembler.toResponse(lifecycleService.completeTrip(actor, bookingId), actor);
    }

    public record AssignDriverRequest(UUID driverId) {
    }
}
