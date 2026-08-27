package com.yocabs.api.modules.pricing.infrastructure.persistence;

import com.yocabs.api.modules.triprequest.domain.model.TripType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingConfigurationJpaRepository
        extends JpaRepository<
        PricingConfigurationEntity,
        UUID
        > {

    Optional<PricingConfigurationEntity>
    findByVehicleIdAndTripType(
            UUID vehicleId,
            TripType tripType
    );

    List<PricingConfigurationEntity>
    findByVehicleId(
            UUID vehicleId
    );
}