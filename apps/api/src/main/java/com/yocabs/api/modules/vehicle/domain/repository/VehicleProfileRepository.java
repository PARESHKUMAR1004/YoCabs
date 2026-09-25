package com.yocabs.api.modules.vehicle.domain.repository;

import com.yocabs.api.modules.vehicle.domain.model.VehicleProfile;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface VehicleProfileRepository {

    VehicleProfile save(VehicleProfile profile);

    Optional<VehicleProfile> find(UUID vehicleId);

    Map<UUID, VehicleProfile> findAll(Collection<UUID> vehicleIds);
}
