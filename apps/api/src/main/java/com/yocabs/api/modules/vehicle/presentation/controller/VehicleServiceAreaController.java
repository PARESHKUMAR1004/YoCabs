package com.yocabs.api.modules.vehicle.presentation.controller;

import com.yocabs.api.modules.vehicle.application.service.VehicleServiceAreaService;
import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/travel-partners/{travelPartnerId}/vehicles/{vehicleId}/service-areas"
)
public class VehicleServiceAreaController {

    private final VehicleServiceAreaService serviceAreaService;

    public VehicleServiceAreaController(
            VehicleServiceAreaService serviceAreaService
    ) {
        this.serviceAreaService = serviceAreaService;
    }

    @GetMapping
    public List<ServiceAreaResponse> list(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {
        actor.requirePartnerAccess(travelPartnerId);

        return serviceAreaService
                .list(travelPartnerId, vehicleId)
                .stream()
                .map(ServiceAreaResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceAreaResponse add(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId,
            @RequestBody ServiceAreaRequest request
    ) {
        actor.requirePartnerAccess(travelPartnerId);

        return ServiceAreaResponse.from(
                serviceAreaService.add(
                        travelPartnerId,
                        vehicleId,
                        request.name(),
                        request.latitude(),
                        request.longitude(),
                        request.radiusKm()
                )
        );
    }

    @DeleteMapping("/{serviceAreaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID serviceAreaId
    ) {
        actor.requirePartnerAccess(travelPartnerId);

        serviceAreaService.remove(
                travelPartnerId,
                vehicleId,
                serviceAreaId
        );
    }

    public record ServiceAreaResponse(
            UUID id,
            String name,
            double latitude,
            double longitude,
            double radiusKm
    ) {
        static ServiceAreaResponse from(ServiceArea area) {
            return new ServiceAreaResponse(
                    area.id(),
                    area.name(),
                    area.latitude(),
                    area.longitude(),
                    area.radiusKm()
            );
        }
    }

    public record ServiceAreaRequest(
            String name,
            double latitude,
            double longitude,
            double radiusKm
    ) {
    }
}
