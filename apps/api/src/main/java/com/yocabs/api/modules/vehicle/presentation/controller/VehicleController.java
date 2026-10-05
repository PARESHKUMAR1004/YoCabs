package com.yocabs.api.modules.vehicle.presentation.controller;

import com.yocabs.api.modules.vehicle.application.service.VehicleService;
import com.yocabs.api.modules.vehicle.application.service.VehicleShowcaseService;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/travel-partners/{travelPartnerId}/vehicles"
)
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleShowcaseService showcaseService;

    public VehicleController(
            VehicleService vehicleService,
            VehicleShowcaseService showcaseService
    ) {
        this.vehicleService =
                vehicleService;
        this.showcaseService = showcaseService;
    }

    private static String photoPath(UUID vehicleId, UUID photoId) {
        return "/api/v1/vehicles/" + vehicleId + "/photos/" + photoId;
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

        List<String> photos =
                showcaseService.forVehicles(List.of(vehicleId)).get(vehicleId).photoIds().stream()
                        .map(photoId -> photoPath(vehicleId, photoId))
                        .toList();

        return VehicleResponse.from(vehicle, photos);
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


        List<Vehicle> vehicles = vehicleService.listVehicles(travelPartnerId);

        Map<UUID, VehicleShowcaseService.Showcase> showcases =
                showcaseService.forVehicles(vehicles.stream().map(Vehicle::getId).toList());

        return vehicles.stream()
                .map(vehicle -> VehicleResponse.from(
                        vehicle,
                        showcases.get(vehicle.getId()).photoIds().stream()
                                .map(photoId -> photoPath(vehicle.getId(), photoId))
                                .toList()
                ))
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
            List<String> photos,
            Instant createdAt,
            Instant updatedAt
    ) {

        /** Used by actions that change status or details but don't touch photos. */
        public static VehicleResponse from(
                Vehicle vehicle
        ) {
            return from(vehicle, List.of());
        }

        public static VehicleResponse from(
                Vehicle vehicle,
                List<String> photos
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
                    photos,
                    vehicle.getCreatedAt(),
                    vehicle.getUpdatedAt()
            );
        }
    }
}