package com.yocabs.api.modules.vehicle.domain.repository;

import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;

import java.util.List;
import java.util.UUID;

public interface VehicleServiceAreaRepository {

    List<ServiceArea> findByVehicleId(UUID vehicleId);

    ServiceArea add(UUID vehicleId, ServiceArea serviceArea);

    void remove(UUID vehicleId, UUID serviceAreaId);
}
