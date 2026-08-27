package com.yocabs.api.modules.pricing.application.service;

import com.yocabs.api.modules.pricing.application.PricingEngine;
import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CalculatePriceService {

    private final PricingEngine pricingEngine;

    private final VehicleRepository vehicleRepository;

    private final PricingConfigurationRepository
            pricingConfigurationRepository;

    public CalculatePriceService(
            PricingEngine pricingEngine,
            VehicleRepository vehicleRepository,
            PricingConfigurationRepository
                    pricingConfigurationRepository
    ) {
        this.pricingEngine =
                pricingEngine;

        this.vehicleRepository =
                vehicleRepository;

        this.pricingConfigurationRepository =
                pricingConfigurationRepository;
    }

    @Transactional(readOnly = true)
    public PriceCalculation calculate(
            UUID vehicleId,
            TripType tripType,
            PricingContext context
    ) {

        if (vehicleId == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (tripType == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Pricing context is required"
            );
        }

        Vehicle vehicle =
                vehicleRepository
                        .findById(vehicleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vehicle not found: "
                                                + vehicleId
                                )
                        );

        PricingConfiguration configuration =
                pricingConfigurationRepository
                        .findByVehicleIdAndTripType(
                                vehicleId,
                                tripType
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pricing configuration not found "
                                                + "for vehicle "
                                                + vehicleId
                                                + " and trip type "
                                                + tripType
                                )
                        );

        return pricingEngine.calculate(
                tripType,
                context,
                vehicle,
                configuration
        );
    }
}