package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VehicleServiceAreaJpaRepository
        extends JpaRepository<VehicleServiceAreaEntity, UUID> {

    List<VehicleServiceAreaEntity> findByVehicleId(UUID vehicleId);

    /** Loads the areas for a whole fleet in one query, so a search never goes vehicle by vehicle. */
    List<VehicleServiceAreaEntity> findByVehicleIdIn(Collection<UUID> vehicleIds);

    void deleteByVehicleId(UUID vehicleId);
}
