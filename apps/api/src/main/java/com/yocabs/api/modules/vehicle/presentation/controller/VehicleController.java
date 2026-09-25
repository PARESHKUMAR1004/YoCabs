package com.yocabs.api.modules.vehicle.presentation.controller;

import com.yocabs.api.modules.vehicle.application.service.VehicleService;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
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
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestBody CreateVehicleRequest request
    ) {

        actor.requirePartnerAccess(travelPartnerId);


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
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        actor.requirePartnerAccess(travelPartnerId);


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
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId,
            @RequestBody UpdateVehicleRequest request
    ) {

        actor.requirePartnerAccess(travelPartnerId);


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
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId
    ) {

        actor.requirePartnerAccess(travelPartnerId);


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
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        actor.requirePartnerAccess(travelPartnerId);


        return VehicleResponse.from(
                vehicleService.makeAvailable(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    @PostMapping("/{vehicleId}/make-unavailable")
    public VehicleResponse makeUnavailable(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        actor.requirePartnerAccess(travelPartnerId);


        return VehicleResponse.from(
                vehicleService.makeUnavailable(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    @PostMapping("/{vehicleId}/maintenance")
    public VehicleResponse sendToMaintenance(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        actor.requirePartnerAccess(travelPartnerId);


        return VehicleResponse.from(
                vehicleService.sendToMaintenance(
                        travelPartnerId,
                        vehicleId
                )
        );
    }

    @PostMapping("/{vehicleId}/deactivate")
    public VehicleResponse deactivate(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {

        actor.requirePartnerAccess(travelPartnerId);


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



    /** Mirrors VehicleServiceAreaController.ServiceAreaResponse so clients see one area shape. */
    public record ServiceAreaResponse(
            UUID id,
            String name,
            double latitude,
            double longitude,
            double radiusKm
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
            List<ServiceAreaResponse> serviceAreas,
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
                    vehicle.getServiceAreas().stream()
                            .map(area -> new ServiceAreaResponse(
                                    area.id(),
                                    area.name(),
                                    area.latitude(),
                                    area.longitude(),
                                    area.radiusKm()
                            ))
                            .toList(),
                    vehicle.getCreatedAt(),
                    vehicle.getUpdatedAt()
            );
        }
    }
}