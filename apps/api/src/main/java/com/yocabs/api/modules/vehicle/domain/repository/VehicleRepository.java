package com.yocabs.api.modules.vehicle.domain.repository;

import com.yocabs.api.modules.vehicle.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {

    Vehicle create(Vehicle vehicle);

    Vehicle update(Vehicle vehicle);

    Optional<Vehicle> findById(UUID id);

    List<Vehicle> findByTravelPartnerId(
            UUID travelPartnerId
    );

    List<Vehicle> findAvailableVehicles();
}