package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.VehicleProfile;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleProfileRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VehicleProfileRepositoryAdapter implements VehicleProfileRepository {

    private final VehicleProfileJpaRepository jpa;

    public VehicleProfileRepositoryAdapter(VehicleProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public VehicleProfile save(VehicleProfile profile) {
        VehicleProfileEntity entity =
                jpa.findById(profile.getVehicleId())
                        .orElseGet(() -> VehicleProfileEntity.fromDomain(profile));

        entity.apply(profile);
        return jpa.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleProfile> find(UUID vehicleId) {
        return jpa.findById(vehicleId).map(VehicleProfileEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, VehicleProfile> findAll(Collection<UUID> vehicleIds) {
        Map<UUID, VehicleProfile> result = new HashMap<>();
        if (vehicleIds.isEmpty()) {
            return result;
        }
        jpa.findAllById(vehicleIds).forEach(entity -> {
            VehicleProfile profile = entity.toDomain();
            result.put(profile.getVehicleId(), profile);
        });
        return result;
    }
}
