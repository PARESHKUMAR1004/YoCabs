package com.yocabs.api.modules.pricing.interfaces.rest;

import com.yocabs.api.modules.pricing.application.PricingAccessGuard;
import com.yocabs.api.modules.pricing.application.service.PricingConfigurationService;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.interfaces.rest.dto.CreatePricingConfigurationRequest;
import com.yocabs.api.modules.pricing.interfaces.rest.dto.PricingConfigurationResponse;
import com.yocabs.api.modules.pricing.interfaces.rest.dto.UpdatePricingConfigurationRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing-configurations")
public class PricingConfigurationController {

    private final PricingConfigurationService
            pricingConfigurationService;

    private final PricingAccessGuard accessGuard;

    public PricingConfigurationController(
            PricingConfigurationService
                    pricingConfigurationService,
            PricingAccessGuard accessGuard
    ) {
        this.pricingConfigurationService =
                pricingConfigurationService;

        this.accessGuard = accessGuard;
    }

    @PostMapping
    public ResponseEntity<PricingConfigurationResponse>
    create(
            @CurrentActor Actor actor,
            @RequestBody
            CreatePricingConfigurationRequest request
    ) {

        accessGuard.requireVehicleAccess(
                actor,
                request.vehicleId()
        );

        PricingConfiguration configuration =
                pricingConfigurationService.create(
                        new PricingConfigurationService
                                .CreatePricingConfigurationCommand(
                                request.vehicleId(),
                                request.tripType(),
                                request.baseFee(),
                                request.perKmCharge(),
                                request.driverAllowance(),
                                request.minimumBillableKm(),
                                request.includedDurationMinutes(),
                                request.includedDistanceKm(),
                                request.packagePrice(),
                                request.extraHourCharge(),
                                request.extraKmCharge()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        PricingConfigurationResponse
                                .fromDomain(configuration)
                );
    }

    @PutMapping("/{id}")
    public PricingConfigurationResponse update(
            @CurrentActor Actor actor,
            @PathVariable UUID id,
            @RequestBody
            UpdatePricingConfigurationRequest request
    ) {

        accessGuard.requireConfigurationAccess(actor, id);
        accessGuard.requireVehicleAccess(actor, request.vehicleId());

        PricingConfiguration configuration =
                pricingConfigurationService.update(
                        id,
                        new PricingConfigurationService
                                .UpdatePricingConfigurationCommand(
                                request.vehicleId(),
                                request.tripType(),
                                request.baseFee(),
                                request.perKmCharge(),
                                request.driverAllowance(),
                                request.minimumBillableKm(),
                                request.includedDurationMinutes(),
                                request.includedDistanceKm(),
                                request.packagePrice(),
                                request.extraHourCharge(),
                                request.extraKmCharge()
                        )
                );

        return PricingConfigurationResponse
                .fromDomain(configuration);
    }

    @GetMapping("/{id}")
    public PricingConfigurationResponse getById(
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {

        accessGuard.requireConfigurationAccess(actor, id);

        return PricingConfigurationResponse
                .fromDomain(
                        pricingConfigurationService
                                .getById(id)
                );
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<PricingConfigurationResponse>
    getByVehicle(
            @CurrentActor Actor actor,
            @PathVariable UUID vehicleId
    ) {

        accessGuard.requireVehicleAccess(actor, vehicleId);

        return pricingConfigurationService
                .getByVehicle(vehicleId)
                .stream()
                .map(
                        PricingConfigurationResponse
                                ::fromDomain
                )
                .toList();
    }

    @GetMapping("/vehicle/{vehicleId}/{tripType}")
    public PricingConfigurationResponse
    getByVehicleAndTripType(
            @CurrentActor Actor actor,
            @PathVariable UUID vehicleId,
            @PathVariable TripType tripType
    ) {

        accessGuard.requireVehicleAccess(actor, vehicleId);

        return PricingConfigurationResponse
                .fromDomain(
                        pricingConfigurationService
                                .getByVehicleAndTripType(
                                        vehicleId,
                                        tripType
                                )
                );
    }

    @PatchMapping("/{id}/activate")
    public PricingConfigurationResponse activate(
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {

        accessGuard.requireConfigurationAccess(actor, id);

        return PricingConfigurationResponse
                .fromDomain(
                        pricingConfigurationService
                                .activate(id)
                );
    }

    @PatchMapping("/{id}/deactivate")
    public PricingConfigurationResponse deactivate(
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {

        accessGuard.requireConfigurationAccess(actor, id);

        return PricingConfigurationResponse
                .fromDomain(
                        pricingConfigurationService
                                .deactivate(id)
                );
    }
}