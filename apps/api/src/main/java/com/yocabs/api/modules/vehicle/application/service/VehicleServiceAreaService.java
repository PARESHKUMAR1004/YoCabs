package com.yocabs.api.modules.vehicle.application.service;

import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleServiceAreaRepository;
import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The ground a single vehicle covers. Areas belong to the vehicle, so a partner can run one car
 * around Bhubaneswar and another out of Puri.
 */
@Service
public class VehicleServiceAreaService {

    private final VehicleRepository vehicleRepository;

    private final VehicleServiceAreaRepository serviceAreaRepository;

    public VehicleServiceAreaService(
            VehicleRepository vehicleRepository,
            VehicleServiceAreaRepository serviceAreaRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.serviceAreaRepository = serviceAreaRepository;
    }

    public List<ServiceArea> list(
            UUID travelPartnerId,
            UUID vehicleId
    ) {
        requireOwnedVehicle(travelPartnerId, vehicleId);

        return serviceAreaRepository.findByVehicleId(vehicleId);
    }

    public ServiceArea add(
            UUID travelPartnerId,
            UUID vehicleId,
            String name,
            double latitude,
            double longitude,
            double radiusKm
    ) {
        requireOwnedVehicle(travelPartnerId, vehicleId);

        return serviceAreaRepository.add(
                vehicleId,
                ServiceArea.create(
                        name,
                        latitude,
                        longitude,
                        radiusKm
                )
        );
    }

    public void remove(
            UUID travelPartnerId,
            UUID vehicleId,
            UUID serviceAreaId
    ) {
        requireOwnedVehicle(travelPartnerId, vehicleId);

        serviceAreaRepository.remove(vehicleId, serviceAreaId);
    }

    /** Stops one partner from editing another partner's vehicle by guessing its id. */
    private void requireOwnedVehicle(
            UUID travelPartnerId,
            UUID vehicleId
    ) {
        Vehicle vehicle =
                vehicleRepository
                        .findById(vehicleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vehicle not found: " + vehicleId
                                )
                        );

        if (!vehicle.getTravelPartnerId().equals(travelPartnerId)) {
            throw new IllegalArgumentException(
                    "Vehicle not found: " + vehicleId
            );
        }
    }
}
