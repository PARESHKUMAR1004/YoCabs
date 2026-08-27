package com.yocabs.api.modules.pricing.infrastructure.persistence;

import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PricingConfigurationRepositoryAdapter
        implements PricingConfigurationRepository {

    private final PricingConfigurationJpaRepository
            jpaRepository;

    public PricingConfigurationRepositoryAdapter(
            PricingConfigurationJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PricingConfiguration create(
            PricingConfiguration configuration
    ) {

        PricingConfigurationEntity entity =
                PricingConfigurationEntity.fromDomain(
                        configuration
                );

        PricingConfigurationEntity saved =
                jpaRepository.save(entity);

        return saved.toDomain();
    }

    @Override
    public PricingConfiguration update(
            PricingConfiguration configuration
    ) {

        PricingConfigurationEntity entity =
                PricingConfigurationEntity.fromDomain(
                        configuration
                );

        PricingConfigurationEntity saved =
                jpaRepository.save(entity);

        return saved.toDomain();
    }

    @Override
    public Optional<PricingConfiguration> findById(
            UUID id
    ) {

        return jpaRepository
                .findById(id)
                .map(PricingConfigurationEntity::toDomain);
    }

    @Override
    public Optional<PricingConfiguration>
    findByVehicleIdAndTripType(
            UUID vehicleId,
            TripType tripType
    ) {

        return jpaRepository
                .findByVehicleIdAndTripType(
                        vehicleId,
                        tripType
                )
                .map(PricingConfigurationEntity::toDomain);
    }

    @Override
    public List<PricingConfiguration>
    findByVehicleId(
            UUID vehicleId
    ) {

        return jpaRepository
                .findByVehicleId(vehicleId)
                .stream()
                .map(PricingConfigurationEntity::toDomain)
                .toList();
    }
}