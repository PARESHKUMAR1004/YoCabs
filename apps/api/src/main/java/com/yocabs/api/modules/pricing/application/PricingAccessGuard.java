package com.yocabs.api.modules.pricing.application;

import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A supplied vehicle/configuration id is not proof of ownership: this checks
 * that the vehicle belongs to the caller's travel partner (or the caller is an admin).
 */
@Component
public class PricingAccessGuard {

    private final VehicleRepository vehicles;
    private final PricingConfigurationRepository configurations;

    public PricingAccessGuard(
            VehicleRepository vehicles,
            PricingConfigurationRepository configurations
    ) {
        this.vehicles = vehicles;
        this.configurations = configurations;
    }

    @Transactional(readOnly = true)
    public void requireVehicleAccess(Actor actor, UUID vehicleId) {

        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle ID is required");
        }

        Vehicle vehicle =
                vehicles.findById(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        actor.requirePartnerAccess(vehicle.getTravelPartnerId());
    }

    @Transactional(readOnly = true)
    public void requireConfigurationAccess(Actor actor, UUID configurationId) {

        PricingConfiguration configuration =
                configurations.findById(configurationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Pricing configuration not found: " + configurationId));

        requireVehicleAccess(actor, configuration.getVehicleId());
    }
}
