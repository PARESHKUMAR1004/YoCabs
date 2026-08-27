package com.yocabs.api.modules.travelpartner.presentation.controller;

import com.yocabs.api.modules.travelpartner.application.command.AddServiceAreaCommand;
import com.yocabs.api.modules.travelpartner.application.command.CreateTravelPartnerCommand;
import com.yocabs.api.modules.travelpartner.application.command.RemoveServiceAreaCommand;
import com.yocabs.api.modules.travelpartner.application.command.UpdateServiceAreaCommand;
import com.yocabs.api.modules.travelpartner.application.service.ActivateTravelPartnerService;
import com.yocabs.api.modules.travelpartner.application.service.AddServiceAreaService;
import com.yocabs.api.modules.travelpartner.application.service.CreateTravelPartnerService;
import com.yocabs.api.modules.travelpartner.application.service.RemoveServiceAreaService;
import com.yocabs.api.modules.travelpartner.application.service.UpdateServiceAreaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/travel-partners")
public class TravelPartnerController {

    private final CreateTravelPartnerService createService;
    private final ActivateTravelPartnerService activateService;
    private final AddServiceAreaService addServiceAreaService;
    private final UpdateServiceAreaService updateServiceAreaService;
    private final RemoveServiceAreaService removeServiceAreaService;

    public TravelPartnerController(
            CreateTravelPartnerService createService,
            ActivateTravelPartnerService activateService,
            AddServiceAreaService addServiceAreaService,
            UpdateServiceAreaService updateServiceAreaService,
            RemoveServiceAreaService removeServiceAreaService
    ) {
        this.createService = createService;
        this.activateService = activateService;
        this.addServiceAreaService = addServiceAreaService;
        this.updateServiceAreaService = updateServiceAreaService;
        this.removeServiceAreaService = removeServiceAreaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(
            @RequestBody CreateTravelPartnerRequest request
    ) {

        return createService
                .execute(
                        new CreateTravelPartnerCommand(
                                request.name()
                        )
                )
                .getId();
    }

    @PostMapping("/{id}/activate")
    public UUID activate(
            @PathVariable UUID id
    ) {

        return activateService
                .execute(id)
                .getId();
    }

    @PostMapping("/{id}/service-areas")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID addServiceArea(
            @PathVariable UUID id,
            @RequestBody ServiceAreaRequest request
    ) {

        return addServiceAreaService
                .execute(
                        new AddServiceAreaCommand(
                                id,
                                request.name(),
                                request.latitude(),
                                request.longitude(),
                                request.radiusKm()
                        )
                )
                .getServiceAreas()
                .getLast()
                .id();
    }

    @PutMapping("/{partnerId}/service-areas/{serviceAreaId}")
    public UUID updateServiceArea(
            @PathVariable UUID partnerId,
            @PathVariable UUID serviceAreaId,
            @RequestBody ServiceAreaRequest request
    ) {

        return updateServiceAreaService
                .execute(
                        new UpdateServiceAreaCommand(
                                partnerId,
                                serviceAreaId,
                                request.name(),
                                request.latitude(),
                                request.longitude(),
                                request.radiusKm()
                        )
                )
                .getId();
    }

    @DeleteMapping("/{partnerId}/service-areas/{serviceAreaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeServiceArea(
            @PathVariable UUID partnerId,
            @PathVariable UUID serviceAreaId
    ) {

        removeServiceAreaService.execute(
                new RemoveServiceAreaCommand(
                        partnerId,
                        serviceAreaId
                )
        );
    }

    public record CreateTravelPartnerRequest(
            String name
    ) {
    }

    public record ServiceAreaRequest(
            String name,
            double latitude,
            double longitude,
            double radiusKm
    ) {
    }
}