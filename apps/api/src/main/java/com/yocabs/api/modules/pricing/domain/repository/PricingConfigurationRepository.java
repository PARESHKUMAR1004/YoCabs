package com.yocabs.api.modules.pricing.domain.repository;

import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingConfigurationRepository {

    PricingConfiguration create(
            PricingConfiguration configuration
    );

    PricingConfiguration update(
            PricingConfiguration configuration
    );

    Optional<PricingConfiguration> findById(
            UUID id
    );

    Optional<PricingConfiguration> findByVehicleIdAndTripType(
            UUID vehicleId,
            TripType tripType
    );

    List<PricingConfiguration> findByVehicleId(
            UUID vehicleId
    );
}