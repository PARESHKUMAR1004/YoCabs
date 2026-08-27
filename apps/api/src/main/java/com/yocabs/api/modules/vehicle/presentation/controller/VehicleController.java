package com.yocabs.api.modules.vehicle.presentation.controller;

import com.yocabs.api.modules.vehicle.application.service.VehicleService;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/travel-partners/{travelPartnerId}/vehicles"
)
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(
            VehicleService vehicleService
    ) {
        this.vehicleService =
                vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(
            @PathVariable UUID travelPartnerId,
            @RequestBody CreateVehicleRequest request
    ) {

        Vehicle vehicle =
                vehicleService.createVehicle(
                        travelPartnerId,
                        request.registrationNumber(),
                        request.make(),
                        request.model(),
                        request.category(),
                        request.passengerCapacity()
                );

        return VehicleResponse.from(
                vehicle
        );
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse get(
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        Vehicle vehicle =
                vehicleService.getVehicle(
                        travelPartnerId,
                        vehicleId
                );

        return VehicleResponse.from(
                vehicle
        );
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse update(
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId,
            @RequestBody UpdateVehicleRequest request
    ) {

        Vehicle vehicle =
                vehicleService.updateVehicle(
                        travelPartnerId,
                        vehicleId,
                        request.registrationNumber(),
                        request.make(),
                        request.model(),
                        request.category(),
                        request.passengerCapacity()
                );

        return VehicleResponse.from(vehicle);
    }


    @GetMapping
    public List<VehicleResponse> list(
            @PathVariable UUID travelPartnerId
    ) {

        return vehicleService
                .listVehicles(
                        travelPartnerId
                )
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @PostMapping("/{vehicleId}/make-available")
    public VehicleResponse makeAvailable(
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        return VehicleResponse.from(
                vehicleService.makeAvailable(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    @PostMapping("/{vehicleId}/make-unavailable")
    public VehicleResponse makeUnavailable(
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        return VehicleResponse.from(
                vehicleService.makeUnavailable(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    @PostMapping("/{vehicleId}/maintenance")
    public VehicleResponse sendToMaintenance(
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        return VehicleResponse.from(
                vehicleService.sendToMaintenance(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    @PostMapping("/{vehicleId}/deactivate")
    public VehicleResponse deactivate(
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        return VehicleResponse.from(
                vehicleService.deactivate(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    public record CreateVehicleRequest(
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity
    ) {
    }

    public record UpdateVehicleRequest(
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity
    ) {
    }



    public record VehicleResponse(
            UUID id,
            UUID travelPartnerId,
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity,
            VehicleStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {

        public static VehicleResponse from(
                Vehicle vehicle
        ) {

            return new VehicleResponse(
                    vehicle.getId(),
                    vehicle.getTravelPartnerId(),
                    vehicle.getRegistrationNumber(),
                    vehicle.getMake(),
                    vehicle.getModel(),
                    vehicle.getCategory(),
                    vehicle.getPassengerCapacity(),
                    vehicle.getStatus(),
                    vehicle.getCreatedAt(),
                    vehicle.getUpdatedAt()
            );
        }
    }
}